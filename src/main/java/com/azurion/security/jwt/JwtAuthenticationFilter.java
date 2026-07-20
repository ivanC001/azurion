package com.azurion.security.jwt;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.usuarios.domain.repositories.UsuarioTenantRolRepository;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UsuarioTenantRolRepository usuarioTenantRolRepository;

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
                Long userId = resolveUserId(claims);
                String tenant = resolveTenantForRequest(tokenTenant, TenantContext.getTenantId(), userId, authorities);

                TenantContext.setTenantId(tenant);
                MDC.put("tenant", tenant);
                MDC.put("userId", String.valueOf(userId));

                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            authorities
                    );
                    auth.setDetails(new TenantAuthenticationDetails(request, userId, tenant));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (AccessDeniedException ex) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"code\":\"TENANT_ACCESS_DENIED\",\"message\":\"No tienes acceso al tenant solicitado\",\"details\":[],\"userActionable\":true}");
                return;
            } catch (Exception ex) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"code\":\"AUTH_ERROR\",\"message\":\"Tu sesion no es valida o ha vencido\",\"details\":[],\"userActionable\":true}");
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
                                           Long userId,
                                           Collection<? extends GrantedAuthority> authorities) {
        boolean canSwitchTenant = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> "ROLE_ADMIN_GENERAL".equals(role) || "ROLE_PLATFORM_ADMIN".equals(role));

        if (canSwitchTenant && requestedTenant != null && !requestedTenant.isBlank()) {
            if (!TenantContext.DEFAULT_TENANT.equalsIgnoreCase(requestedTenant)
                    && !requestedTenant.equalsIgnoreCase(tokenTenant)
                    && !usuarioTenantRolRepository
                    .existsByUsuarioGlobalIdAndTenantIdIgnoreCaseAndActivoTrue(userId, requestedTenant)) {
                throw new AccessDeniedException("Usuario global sin asignacion al tenant solicitado");
            }
            return requestedTenant;
        }
        return tokenTenant == null || tokenTenant.isBlank() ? TenantContext.DEFAULT_TENANT : tokenTenant;
    }

    private Long resolveUserId(Claims claims) {
        Object raw = claims.getOrDefault("userId", claims.get("uid"));
        if (raw instanceof Number number) {
            return number.longValue();
        }
        if (raw == null || !String.valueOf(raw).matches("^\\d+$")) {
            throw new IllegalArgumentException("JWT sin userId valido");
        }
        return Long.parseLong(String.valueOf(raw));
    }
}
