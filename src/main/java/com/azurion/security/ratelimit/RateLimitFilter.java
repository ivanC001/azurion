package com.azurion.security.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final Set<String> AUTH_PATHS = Set.of(
            "/v1/auth/public/login",
            "/v1/auth/tenant/login"
    );
    private static final Set<String> CALLBACK_PREFIXES = Set.of(
            "/v1/facturador/callback/"
    );

    private final boolean enabled;
    private final int authLimit;
    private final int publicLimit;
    private final boolean distributedEnabled;
    private final RequestRateLimiter rateLimiter;
    private final DistributedRequestRateLimiter distributedRateLimiter;
    private final ClientIpResolver clientIpResolver;

    public RateLimitFilter(
            @Value("${azurion.security.rate-limit.enabled:true}") boolean enabled,
            @Value("${azurion.security.rate-limit.auth-requests-per-minute:10}") int authLimit,
            @Value("${azurion.security.rate-limit.public-requests-per-minute:60}") int publicLimit,
            @Value("${azurion.security.rate-limit.max-clients:10000}") int maxClients,
            @Value("${azurion.security.rate-limit.distributed-enabled:true}") boolean distributedEnabled,
            DistributedRequestRateLimiter distributedRateLimiter,
            ClientIpResolver clientIpResolver
    ) {
        this.enabled = enabled;
        this.authLimit = positive(authLimit, "auth-requests-per-minute");
        this.publicLimit = positive(publicLimit, "public-requests-per-minute");
        this.distributedEnabled = distributedEnabled;
        this.rateLimiter = new RequestRateLimiter(positive(maxClients, "max-clients"));
        this.distributedRateLimiter = distributedRateLimiter;
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled || HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String path = request.getServletPath();
        return !AUTH_PATHS.contains(path) && !isProtectedPublicPath(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getServletPath();
        boolean authRequest = AUTH_PATHS.contains(path);
        int limit = authRequest ? authLimit : publicLimit;
        String bucket = authRequest ? "auth" : bucketFor(path);
        String client = clientIpResolver.resolve(request);

        String key = bucket + ':' + client;
        RequestRateLimiter.Decision decision = acquire(key, limit);
        if (!decision.allowed()) {
            response.setStatus(429);
            response.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"code\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Demasiadas solicitudes. Intenta nuevamente mas tarde.\",\"details\":[],\"userActionable\":true}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isProtectedPublicPath(String path) {
        return path.equals("/v1/public/crm/leads")
                || path.equals("/public/crm/leads")
                || path.startsWith("/v1/public/crm/catalogo/")
                || path.startsWith("/public/crm/catalogo/")
                || path.startsWith("/v1/public/crm/whatsapp/")
                || path.startsWith("/public/crm/whatsapp/")
                || CALLBACK_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private RequestRateLimiter.Decision acquire(String key, int limit) {
        if (!distributedEnabled) {
            return rateLimiter.tryAcquire(key, limit, WINDOW);
        }
        try {
            return distributedRateLimiter.tryAcquire(key, limit, WINDOW);
        } catch (RuntimeException storageFailure) {
            log.error("Rate limiter distribuido no disponible; se usa proteccion local temporal", storageFailure);
            return rateLimiter.tryAcquire(key, limit, WINDOW);
        }
    }

    private String bucketFor(String path) {
        if (CALLBACK_PREFIXES.stream().anyMatch(path::startsWith)) {
            return "facturador-callback";
        }
        if (path.contains("/whatsapp/")) {
            return "public-whatsapp";
        }
        return "public-crm";
    }

    private static int positive(int value, String property) {
        if (value < 1) {
            throw new IllegalArgumentException("azurion.security.rate-limit." + property + " debe ser mayor que cero");
        }
        return value;
    }
}
