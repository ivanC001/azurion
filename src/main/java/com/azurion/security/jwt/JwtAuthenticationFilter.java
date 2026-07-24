package com.azurion.security.jwt;

import com.azurion.multitenancy.TenantContext;
import com.azurion.security.session.AuthSessionService;
import com.azurion.security.session.SessionRevokedException;
import com.azurion.security.session.SessionStoreUnavailableException;
import com.azurion.security.session.SessionClientInfo;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    private final AuthSessionService authSessionService;

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
                String sessionId = String.valueOf(claims.getOrDefault("sid", ""));
                authSessionService.validate(
                        tokenTenant,
                        userId,
                        sessionId,
                        SessionClientInfo.from(
                                request,
                                "unknown",
                                "Navegador de la sesion"
                        )
                );
                String tenant = resolveTenantForRequest(tokenTenant, TenantContext.getTenantId(), authorities);

                TenantContext.setTenantId(tenant);
                MDC.put("tenant", tenant);
                MDC.put("userId", String.valueOf(userId));

                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            authorities
                    );
                    auth.setDetails(new TenantAuthenticationDetails(
                            request,
                            userId,
                            tenant,
                            tokenTenant,
                            sessionId
                    ));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (SessionStoreUnavailableException ex) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
                response.setContentType("application/json");
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getWriter().write("{\"code\":\"SESSION_SERVICE_UNAVAILABLE\",\"message\":\"El servicio de sesiones no esta disponible. Intenta nuevamente en unos minutos.\",\"details\":[],\"userActionable\":true}");
                return;
            } catch (SessionRevokedException ex) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json");
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getWriter().write("{\"code\":\"SESSION_REVOKED\",\"message\":\"La sesión ya no está activa\",\"details\":[],\"userActionable\":true}");
                return;
            } catch (AccessDeniedException ex) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType("application/json");
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getWriter().write("{\"code\":\"TENANT_ACCESS_DENIED\",\"message\":\"No tienes acceso al tenant solicitado\",\"details\":[],\"userActionable\":true}");
                return;
            } catch (Exception ex) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json");
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
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
                                           Collection<? extends GrantedAuthority> authorities) {
        String normalizedTokenTenant = tokenTenant == null || tokenTenant.isBlank()
                ? TenantContext.DEFAULT_TENANT
                : tokenTenant;
        boolean canSwitchTenant = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> "ROLE_ADMIN_GENERAL".equals(role) || "ROLE_PLATFORM_ADMIN".equals(role));

        if (canSwitchTenant && requestedTenant != null && !requestedTenant.isBlank()) {
            return requestedTenant;
        }

        if (requestedTenant != null
                && !requestedTenant.isBlank()
                && !TenantContext.DEFAULT_TENANT.equalsIgnoreCase(requestedTenant)
                && !requestedTenant.equalsIgnoreCase(normalizedTokenTenant)) {
            throw new AccessDeniedException("Usuario sin acceso al tenant solicitado");
        }

        return normalizedTokenTenant;
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
