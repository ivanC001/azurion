package com.azurion.security.jwt;

import com.azurion.multitenancy.TenantContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            try {
                String token = header.substring(7);
                Claims claims = jwtTokenProvider.parseClaims(token);

                String username = claims.getSubject();
                Collection<? extends GrantedAuthority> authorities = jwtTokenProvider.authoritiesFrom(claims);
                String tokenTenant = String.valueOf(claims.getOrDefault("tenant", TenantContext.DEFAULT_TENANT));
                String tenant = resolveTenantForRequest(tokenTenant, TenantContext.getTenantId(), authorities);
                String userId = String.valueOf(claims.getOrDefault("userId", claims.getOrDefault("uid", "unknown")));

                TenantContext.setTenantId(tenant);
                MDC.put("tenant", tenant);
                MDC.put("userId", userId);

                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            authorities
                    );
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception ex) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"code\":\"AUTH_ERROR\",\"message\":\"Invalid JWT token\"}");
                return;
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("userId");
        }
    }

    private String resolveTenantForRequest(String tokenTenant,
                                           String requestedTenant,
                                           Collection<? extends GrantedAuthority> authorities) {
        boolean canSwitchTenant = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> "ROLE_ADMIN_GENERAL".equals(role) || "ROLE_PLATFORM_ADMIN".equals(role));

        if (canSwitchTenant && requestedTenant != null && !requestedTenant.isBlank()) {
            return requestedTenant;
        }
        return tokenTenant == null || tokenTenant.isBlank() ? TenantContext.DEFAULT_TENANT : tokenTenant;
    }
}
