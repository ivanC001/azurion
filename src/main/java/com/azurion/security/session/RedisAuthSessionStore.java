package com.azurion.security.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(
        prefix = "azurion.security.sessions",
        name = "store",
        havingValue = "redis"
)
@RequiredArgsConstructor
public class RedisAuthSessionStore implements AuthSessionStore {

    private static final String SESSION_PREFIX = "auth:session:";
    private static final String USER_PREFIX = "auth:user:";
    private static final String REPLACEMENT_PREFIX = "auth:replacement:";

    private static final DefaultRedisScript<String> RESERVE_SCRIPT = script("""
            local current = redis.call('GET', KEYS[1])
            if current then
              local currentKey = ARGV[1] .. current
              if redis.call('EXISTS', currentKey) == 1
                  and redis.call('HGET', currentKey, 'state') == 'ACTIVE' then
                if redis.call('HGET', currentKey, 'deviceId') == ARGV[5] then
                  return 'SAME_DEVICE:' .. current
                end
                return 'CONFLICT:' .. current
              end
              redis.call('DEL', KEYS[1])
            end
            redis.call('HSET', KEYS[2],
              'sessionId', ARGV[2], 'tenantId', ARGV[3], 'userId', ARGV[4],
              'deviceId', ARGV[5], 'deviceName', ARGV[6],
              'createdAt', ARGV[7], 'lastActivityAt', ARGV[8],
              'expiresAt', ARGV[9], 'state', 'ACTIVE')
            redis.call('PEXPIRE', KEYS[2], ARGV[10])
            redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[10])
            return 'CREATED:' .. ARGV[2]
            """);

    private static final DefaultRedisScript<String> REPLACE_SCRIPT = script("""
            if redis.call('EXISTS', KEYS[3]) == 0 then
              return 'REPLACEMENT_TOKEN_INVALID'
            end
            local current = redis.call('GET', KEYS[1])
            if not current or current ~= ARGV[2] then
              redis.call('DEL', KEYS[3])
              return 'REPLACEMENT_TOKEN_STALE'
            end
            local previousKey = ARGV[1] .. ARGV[2]
            if redis.call('EXISTS', previousKey) == 0
                or redis.call('HGET', previousKey, 'state') ~= 'ACTIVE' then
              redis.call('DEL', KEYS[3])
              return 'REPLACEMENT_TOKEN_STALE'
            end
            redis.call('HSET', previousKey, 'state', 'REVOKED', 'revokedAt', ARGV[8])
            redis.call('HSET', KEYS[2],
              'sessionId', ARGV[3], 'tenantId', ARGV[4], 'userId', ARGV[5],
              'deviceId', ARGV[6], 'deviceName', ARGV[7],
              'createdAt', ARGV[8], 'lastActivityAt', ARGV[8],
              'expiresAt', ARGV[9], 'state', 'ACTIVE')
            redis.call('PEXPIRE', KEYS[2], ARGV[10])
            redis.call('SET', KEYS[1], ARGV[3], 'PX', ARGV[10])
            redis.call('DEL', KEYS[3])
            return 'CREATED'
            """);

    private static final DefaultRedisScript<String> VALIDATE_SCRIPT = script("""
            if redis.call('EXISTS', KEYS[2]) == 0 then return 'REVOKED' end
            if redis.call('GET', KEYS[1]) ~= ARGV[1] then return 'REVOKED' end
            if redis.call('HGET', KEYS[2], 'state') ~= 'ACTIVE' then return 'REVOKED' end
            if redis.call('HGET', KEYS[2], 'tenantId') ~= ARGV[2] then return 'REVOKED' end
            if redis.call('HGET', KEYS[2], 'userId') ~= ARGV[3] then return 'REVOKED' end
            local last = tonumber(redis.call('HGET', KEYS[2], 'lastActivityAt') or '0')
            if tonumber(ARGV[4]) - last >= tonumber(ARGV[5]) then
              redis.call('HSET', KEYS[2], 'lastActivityAt', ARGV[4])
            end
            return 'ACTIVE'
            """);

    private static final DefaultRedisScript<Long> REVOKE_SCRIPT =
            new DefaultRedisScript<>("""
                    if redis.call('EXISTS', KEYS[2]) == 0 then return 0 end
                    redis.call('HSET', KEYS[2], 'state', 'REVOKED', 'revokedAt', ARGV[2])
                    if redis.call('GET', KEYS[1]) == ARGV[1] then
                      redis.call('DEL', KEYS[1])
                    end
                    return 1
                    """, Long.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Override
    public ReservationResult reserve(AuthSessionRecord candidate, Duration ttl) {
        try {
            String result = redis.execute(
                    RESERVE_SCRIPT,
                    List.of(userKey(candidate.tenantId(), candidate.userId()), sessionKey(candidate.sessionId())),
                    SESSION_PREFIX,
                    candidate.sessionId(),
                    candidate.tenantId(),
                    String.valueOf(candidate.userId()),
                    candidate.deviceId(),
                    candidate.deviceName(),
                    millis(candidate.createdAt()),
                    millis(candidate.lastActivityAt()),
                    millis(candidate.expiresAt()),
                    String.valueOf(ttl.toMillis())
            );
            String[] parts = required(result).split(":", 2);
            return new ReservationResult(ReservationResult.Status.valueOf(parts[0]), parts[1]);
        } catch (DataAccessException | IllegalStateException ex) {
            throw unavailable(ex);
        }
    }

    @Override
    public Optional<AuthSessionRecord> find(String tenantId, Long userId, String sessionId) {
        try {
            Map<Object, Object> values = redis.opsForHash().entries(sessionKey(sessionId));
            if (values.isEmpty()
                    || !tenantId.equals(String.valueOf(values.get("tenantId")))
                    || !String.valueOf(userId).equals(String.valueOf(values.get("userId")))) {
                return Optional.empty();
            }
            return Optional.of(toRecord(values));
        } catch (DataAccessException | IllegalArgumentException ex) {
            throw unavailable(ex);
        }
    }

    @Override
    public void saveReplacement(String tokenHash, ReplacementChallenge challenge, Duration ttl) {
        try {
            redis.opsForValue().set(replacementKey(tokenHash), objectMapper.writeValueAsString(challenge), ttl);
        } catch (DataAccessException | JsonProcessingException ex) {
            throw unavailable(ex);
        }
    }

    @Override
    public Optional<ReplacementChallenge> findReplacement(String tokenHash) {
        try {
            String value = redis.opsForValue().get(replacementKey(tokenHash));
            return value == null
                    ? Optional.empty()
                    : Optional.of(objectMapper.readValue(value, ReplacementChallenge.class));
        } catch (DataAccessException | JsonProcessingException ex) {
            throw unavailable(ex);
        }
    }

    @Override
    public AuthSessionRecord replace(
            String tokenHash,
            ReplacementChallenge challenge,
            AuthSessionRecord candidate,
            Duration ttl
    ) {
        try {
            String result = redis.execute(
                    REPLACE_SCRIPT,
                    List.of(
                            userKey(candidate.tenantId(), candidate.userId()),
                            sessionKey(candidate.sessionId()),
                            replacementKey(tokenHash)
                    ),
                    SESSION_PREFIX,
                    challenge.previousSessionId(),
                    candidate.sessionId(),
                    candidate.tenantId(),
                    String.valueOf(candidate.userId()),
                    candidate.deviceId(),
                    candidate.deviceName(),
                    millis(candidate.createdAt()),
                    millis(candidate.expiresAt()),
                    String.valueOf(ttl.toMillis())
            );
            if (!"CREATED".equals(result)) {
                throw new IllegalArgumentException(result);
            }
            return candidate;
        } catch (DataAccessException ex) {
            throw unavailable(ex);
        }
    }

    @Override
    public ValidationResult validateAndTouch(
            String tenantId,
            Long userId,
            String sessionId,
            Instant now,
            Duration touchInterval
    ) {
        try {
            String result = redis.execute(
                    VALIDATE_SCRIPT,
                    List.of(userKey(tenantId, userId), sessionKey(sessionId)),
                    sessionId,
                    tenantId,
                    String.valueOf(userId),
                    millis(now),
                    String.valueOf(touchInterval.toMillis())
            );
            return "ACTIVE".equals(result) ? ValidationResult.ACTIVE : ValidationResult.REVOKED;
        } catch (DataAccessException ex) {
            throw unavailable(ex);
        }
    }

    @Override
    public boolean revoke(String tenantId, Long userId, String sessionId, Instant revokedAt) {
        try {
            Long result = redis.execute(
                    REVOKE_SCRIPT,
                    List.of(userKey(tenantId, userId), sessionKey(sessionId)),
                    sessionId,
                    millis(revokedAt)
            );
            return result != null && result == 1L;
        } catch (DataAccessException ex) {
            throw unavailable(ex);
        }
    }

    private AuthSessionRecord toRecord(Map<Object, Object> values) {
        return new AuthSessionRecord(
                value(values, "sessionId"),
                value(values, "tenantId"),
                Long.valueOf(value(values, "userId")),
                value(values, "deviceId"),
                value(values, "deviceName"),
                Instant.ofEpochMilli(Long.parseLong(value(values, "createdAt"))),
                Instant.ofEpochMilli(Long.parseLong(value(values, "lastActivityAt"))),
                Instant.ofEpochMilli(Long.parseLong(value(values, "expiresAt"))),
                value(values, "state")
        );
    }

    private String value(Map<Object, Object> values, String name) {
        Object value = values.get(name);
        if (value == null) {
            throw new IllegalArgumentException("Sesion incompleta: " + name);
        }
        return String.valueOf(value);
    }

    private String userKey(String tenantId, Long userId) {
        return USER_PREFIX + tenantId + ":" + userId + ":active-session";
    }

    private String sessionKey(String sessionId) {
        return SESSION_PREFIX + sessionId;
    }

    private String replacementKey(String tokenHash) {
        return REPLACEMENT_PREFIX + tokenHash;
    }

    private String millis(Instant value) {
        return String.valueOf(value.toEpochMilli());
    }

    private String required(String value) {
        if (value == null || !value.contains(":")) {
            throw new IllegalStateException("Respuesta inesperada de Redis");
        }
        return value;
    }

    private SessionStoreUnavailableException unavailable(Exception ex) {
        return new SessionStoreUnavailableException(ex);
    }

    private static DefaultRedisScript<String> script(String source) {
        return new DefaultRedisScript<>(source, String.class);
    }
}
