package com.azurion.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RequestRateLimiterTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void blocksRequestsAboveTheWindowLimit() {
        RequestRateLimiter limiter = new RequestRateLimiter(10, CLOCK);

        assertThat(limiter.tryAcquire("auth:127.0.0.1", 2, Duration.ofMinutes(1)).allowed()).isTrue();
        assertThat(limiter.tryAcquire("auth:127.0.0.1", 2, Duration.ofMinutes(1)).allowed()).isTrue();
        RequestRateLimiter.Decision blocked = limiter.tryAcquire("auth:127.0.0.1", 2, Duration.ofMinutes(1));

        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.retryAfterSeconds()).isEqualTo(60);
    }

    @Test
    void refusesNewBucketsWhenTheClientCapIsReached() {
        RequestRateLimiter limiter = new RequestRateLimiter(1, CLOCK);

        assertThat(limiter.tryAcquire("first", 1, Duration.ofMinutes(1)).allowed()).isTrue();
        assertThat(limiter.tryAcquire("second", 1, Duration.ofMinutes(1)).allowed()).isFalse();
    }
}
