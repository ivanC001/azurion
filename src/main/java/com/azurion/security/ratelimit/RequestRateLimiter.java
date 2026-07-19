package com.azurion.security.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Fixed-window limiter with a hard cap on tracked clients. It deliberately uses
 * the socket remote address; forwarded headers are ignored unless the deployment
 * terminates them in a trusted proxy before reaching this application.
 */
public class RequestRateLimiter {

    private static final int CLEANUP_INTERVAL = 256;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicLong operations = new AtomicLong();
    private final Clock clock;
    private final int maxClients;

    public RequestRateLimiter(int maxClients) {
        this(maxClients, Clock.systemUTC());
    }

    RequestRateLimiter(int maxClients, Clock clock) {
        if (maxClients < 1) {
            throw new IllegalArgumentException("maxClients debe ser mayor que cero");
        }
        this.maxClients = maxClients;
        this.clock = clock;
    }

    public Decision tryAcquire(String key, int limit, Duration duration) {
        if (limit < 1 || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("El limite y la ventana deben ser positivos");
        }

        long now = clock.millis();
        cleanupIfNeeded(now);
        if (!windows.containsKey(key) && windows.size() >= maxClients) {
            cleanupExpired(now);
            if (windows.size() >= maxClients) {
                return new Decision(false, duration.toSeconds());
            }
        }

        boolean[] allowed = {false};
        long[] retryAfterSeconds = {duration.toSeconds()};
        windows.compute(key, (ignored, current) -> {
            long expiresAt = now + duration.toMillis();
            if (current == null || current.expiresAtMillis() <= now) {
                allowed[0] = true;
                return new Window(1, expiresAt);
            }
            retryAfterSeconds[0] = Math.max(1, (current.expiresAtMillis() - now + 999) / 1000);
            if (current.count() >= limit) {
                return current;
            }
            allowed[0] = true;
            return new Window(current.count() + 1, current.expiresAtMillis());
        });
        return new Decision(allowed[0], retryAfterSeconds[0]);
    }

    private void cleanupIfNeeded(long now) {
        if (operations.incrementAndGet() % CLEANUP_INTERVAL == 0) {
            cleanupExpired(now);
        }
    }

    private void cleanupExpired(long now) {
        windows.entrySet().removeIf(entry -> entry.getValue().expiresAtMillis() <= now);
    }

    private record Window(int count, long expiresAtMillis) {
    }

    public record Decision(boolean allowed, long retryAfterSeconds) {
    }
}
