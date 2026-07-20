package com.azurion.security.ratelimit;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedRequestRateLimiter {

    private final JdbcTemplate jdbcTemplate;
    private final PlatformTransactionManager transactionManager;

    @Value("${azurion.security.rate-limit.distributed-enabled:true}")
    private boolean enabled;

    public RequestRateLimiter.Decision tryAcquire(String key, int limit, Duration window) {
        RequestRateLimiter.Decision decision = new TransactionTemplate(transactionManager).execute(status -> {
            lockKey(key);
            Instant now = Instant.now();
            Instant cutoff = now.minus(window);
            jdbcTemplate.update(
                    "delete from public.request_rate_limit_events where bucket_key = ? and occurred_at <= ?",
                    key,
                    Timestamp.from(cutoff)
            );

            Integer count = jdbcTemplate.queryForObject(
                    "select count(*) from public.request_rate_limit_events where bucket_key = ? and occurred_at > ?",
                    Integer.class,
                    key,
                    Timestamp.from(cutoff)
            );
            if (count != null && count >= limit) {
                Timestamp oldest = jdbcTemplate.queryForObject(
                        "select min(occurred_at) from public.request_rate_limit_events where bucket_key = ? and occurred_at > ?",
                        Timestamp.class,
                        key,
                        Timestamp.from(cutoff)
                );
                long retry = oldest == null
                        ? window.toSeconds()
                        : Math.max(1, Duration.between(now, oldest.toInstant().plus(window)).toSeconds());
                return new RequestRateLimiter.Decision(false, retry);
            }

            jdbcTemplate.update(
                    "insert into public.request_rate_limit_events(bucket_key, occurred_at) values (?, ?)",
                    key,
                    Timestamp.from(now)
            );
            return new RequestRateLimiter.Decision(true, window.toSeconds());
        });
        return decision == null ? new RequestRateLimiter.Decision(false, window.toSeconds()) : decision;
    }

    @Scheduled(fixedDelayString = "${azurion.security.rate-limit.cleanup-delay-millis:60000}")
    public void cleanupExpired() {
        if (!enabled) {
            return;
        }
        try {
            jdbcTemplate.update(
                    "delete from public.request_rate_limit_events where occurred_at < ?",
                    Timestamp.from(Instant.now().minus(Duration.ofMinutes(10)))
            );
        } catch (RuntimeException error) {
            log.warn("No se pudo limpiar eventos antiguos del rate limiter", error);
        }
    }

    private void lockKey(String key) {
        jdbcTemplate.query(
                "select pg_advisory_xact_lock(hashtextextended(?, 0))",
                statement -> statement.setString(1, key),
                resultSet -> null
        );
    }
}
