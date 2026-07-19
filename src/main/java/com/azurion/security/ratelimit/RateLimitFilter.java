package com.azurion.security.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final Set<String> AUTH_PATHS = Set.of(
            "/v1/auth/public/login",
            "/v1/auth/tenant/login"
    );

    private final boolean enabled;
    private final int authLimit;
    private final int publicLimit;
    private final RequestRateLimiter rateLimiter;
    private final ClientIpResolver clientIpResolver;

    public RateLimitFilter(
            @Value("${azurion.security.rate-limit.enabled:true}") boolean enabled,
            @Value("${azurion.security.rate-limit.auth-requests-per-minute:10}") int authLimit,
            @Value("${azurion.security.rate-limit.public-requests-per-minute:60}") int publicLimit,
            @Value("${azurion.security.rate-limit.max-clients:10000}") int maxClients,
            ClientIpResolver clientIpResolver
    ) {
        this.enabled = enabled;
        this.authLimit = positive(authLimit, "auth-requests-per-minute");
        this.publicLimit = positive(publicLimit, "public-requests-per-minute");
        this.rateLimiter = new RequestRateLimiter(positive(maxClients, "max-clients"));
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled || HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String path = request.getServletPath();
        return !AUTH_PATHS.contains(path) && !isPublicCrmPath(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getServletPath();
        boolean authRequest = AUTH_PATHS.contains(path);
        int limit = authRequest ? authLimit : publicLimit;
        String bucket = authRequest ? "auth" : "public-crm";
        String client = clientIpResolver.resolve(request);

        RequestRateLimiter.Decision decision = rateLimiter.tryAcquire(bucket + ':' + client, limit, WINDOW);
        if (!decision.allowed()) {
            response.setStatus(429);
            response.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"code\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Demasiadas solicitudes. Intenta nuevamente mas tarde.\",\"details\":[],\"userActionable\":true}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isPublicCrmPath(String path) {
        return path.equals("/v1/public/crm/leads")
                || path.equals("/public/crm/leads")
                || path.startsWith("/v1/public/crm/catalogo/")
                || path.startsWith("/public/crm/catalogo/");
    }

    private static int positive(int value, String property) {
        if (value < 1) {
            throw new IllegalArgumentException("azurion.security.rate-limit." + property + " debe ser mayor que cero");
        }
        return value;
    }
}
