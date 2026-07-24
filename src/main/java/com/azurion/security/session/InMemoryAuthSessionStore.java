package com.azurion.security.session;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(
        prefix = "azurion.security.sessions",
        name = "store",
        havingValue = "memory",
        matchIfMissing = true
)
public class InMemoryAuthSessionStore implements AuthSessionStore {

    private final Map<String, String> activeSessionByUser = new HashMap<>();
    private final Map<String, AuthSessionRecord> sessions = new HashMap<>();
    private final Map<String, ExpiringReplacement> replacements = new HashMap<>();

    @Override
    public synchronized ReservationResult reserve(AuthSessionRecord candidate, Duration ttl) {
        Instant now = Instant.now();
        cleanupExpired(now);

        String userKey = userKey(candidate.tenantId(), candidate.userId());
        String currentSessionId = activeSessionByUser.get(userKey);
        AuthSessionRecord current = currentSessionId == null ? null : sessions.get(currentSessionId);
        if (isActive(current, now)) {
            ReservationResult.Status status = current.deviceId().equals(candidate.deviceId())
                    ? ReservationResult.Status.SAME_DEVICE
                    : ReservationResult.Status.CONFLICT;
            return new ReservationResult(status, current.sessionId());
        }

        activeSessionByUser.remove(userKey);
        sessions.put(candidate.sessionId(), candidate);
        activeSessionByUser.put(userKey, candidate.sessionId());
        return new ReservationResult(ReservationResult.Status.CREATED, candidate.sessionId());
    }

    @Override
    public synchronized Optional<AuthSessionRecord> find(String tenantId, Long userId, String sessionId) {
        Instant now = Instant.now();
        cleanupExpired(now);
        AuthSessionRecord session = sessions.get(sessionId);
        if (session == null
                || !tenantId.equals(session.tenantId())
                || !userId.equals(session.userId())) {
            return Optional.empty();
        }
        return Optional.of(session);
    }

    @Override
    public synchronized void saveReplacement(
            String tokenHash,
            ReplacementChallenge challenge,
            Duration ttl
    ) {
        replacements.put(tokenHash, new ExpiringReplacement(challenge, Instant.now().plus(ttl)));
    }

    @Override
    public synchronized Optional<ReplacementChallenge> findReplacement(String tokenHash) {
        Instant now = Instant.now();
        cleanupExpired(now);
        ExpiringReplacement replacement = replacements.get(tokenHash);
        return replacement == null ? Optional.empty() : Optional.of(replacement.challenge());
    }

    @Override
    public synchronized AuthSessionRecord replace(
            String tokenHash,
            ReplacementChallenge challenge,
            AuthSessionRecord candidate,
            Duration ttl
    ) {
        Instant now = Instant.now();
        cleanupExpired(now);

        ExpiringReplacement replacement = replacements.get(tokenHash);
        if (replacement == null) {
            throw new IllegalArgumentException("REPLACEMENT_TOKEN_INVALID");
        }

        String userKey = userKey(candidate.tenantId(), candidate.userId());
        String currentSessionId = activeSessionByUser.get(userKey);
        if (!challenge.previousSessionId().equals(currentSessionId)) {
            replacements.remove(tokenHash);
            throw new IllegalArgumentException("REPLACEMENT_TOKEN_STALE");
        }

        AuthSessionRecord previous = sessions.get(challenge.previousSessionId());
        if (!isActive(previous, now)) {
            replacements.remove(tokenHash);
            throw new IllegalArgumentException("REPLACEMENT_TOKEN_STALE");
        }

        sessions.put(previous.sessionId(), withState(previous, AuthSessionRecord.REVOKED));
        sessions.put(candidate.sessionId(), candidate);
        activeSessionByUser.put(userKey, candidate.sessionId());
        replacements.remove(tokenHash);
        return candidate;
    }

    @Override
    public synchronized ValidationResult validateAndTouch(
            String tenantId,
            Long userId,
            String sessionId,
            Instant now,
            Duration touchInterval
    ) {
        cleanupExpired(now);
        String userKey = userKey(tenantId, userId);
        AuthSessionRecord session = sessions.get(sessionId);
        if (!sessionId.equals(activeSessionByUser.get(userKey))
                || !isActive(session, now)
                || !tenantId.equals(session.tenantId())
                || !userId.equals(session.userId())) {
            return ValidationResult.REVOKED;
        }

        if (Duration.between(session.lastActivityAt(), now).compareTo(touchInterval) >= 0) {
            sessions.put(sessionId, withLastActivity(session, now));
        }
        return ValidationResult.ACTIVE;
    }

    @Override
    public synchronized boolean revoke(
            String tenantId,
            Long userId,
            String sessionId,
            Instant revokedAt
    ) {
        cleanupExpired(revokedAt);
        AuthSessionRecord session = sessions.get(sessionId);
        if (session == null
                || !tenantId.equals(session.tenantId())
                || !userId.equals(session.userId())) {
            return false;
        }

        sessions.put(sessionId, withState(session, AuthSessionRecord.REVOKED));
        activeSessionByUser.remove(userKey(tenantId, userId), sessionId);
        return true;
    }

    private void cleanupExpired(Instant now) {
        sessions.entrySet().removeIf(entry -> {
            AuthSessionRecord session = entry.getValue();
            if (session.expiresAt().isAfter(now)) {
                return false;
            }
            activeSessionByUser.remove(userKey(session.tenantId(), session.userId()), session.sessionId());
            return true;
        });
        replacements.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    private boolean isActive(AuthSessionRecord session, Instant now) {
        return session != null
                && AuthSessionRecord.ACTIVE.equals(session.state())
                && session.expiresAt().isAfter(now);
    }

    private String userKey(String tenantId, Long userId) {
        return tenantId + ':' + userId;
    }

    private AuthSessionRecord withState(AuthSessionRecord session, String state) {
        return new AuthSessionRecord(
                session.sessionId(),
                session.tenantId(),
                session.userId(),
                session.deviceId(),
                session.deviceName(),
                session.createdAt(),
                session.lastActivityAt(),
                session.expiresAt(),
                state
        );
    }

    private AuthSessionRecord withLastActivity(AuthSessionRecord session, Instant lastActivityAt) {
        return new AuthSessionRecord(
                session.sessionId(),
                session.tenantId(),
                session.userId(),
                session.deviceId(),
                session.deviceName(),
                session.createdAt(),
                lastActivityAt,
                session.expiresAt(),
                session.state()
        );
    }

    private record ExpiringReplacement(
            ReplacementChallenge challenge,
            Instant expiresAt
    ) {
    }
}
