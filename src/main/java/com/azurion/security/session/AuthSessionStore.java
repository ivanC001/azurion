package com.azurion.security.session;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public interface AuthSessionStore {

    ReservationResult reserve(AuthSessionRecord candidate, Duration ttl);

    Optional<AuthSessionRecord> find(String tenantId, Long userId, String sessionId);

    void saveReplacement(String tokenHash, ReplacementChallenge challenge, Duration ttl);

    Optional<ReplacementChallenge> findReplacement(String tokenHash);

    AuthSessionRecord replace(
            String tokenHash,
            ReplacementChallenge challenge,
            AuthSessionRecord candidate,
            Duration ttl
    );

    ValidationResult validateAndTouch(
            String tenantId,
            Long userId,
            String sessionId,
            Instant now,
            Duration touchInterval
    );

    boolean revoke(String tenantId, Long userId, String sessionId, Instant revokedAt);

    record ReservationResult(Status status, String sessionId) {
        public enum Status {
            CREATED,
            SAME_DEVICE,
            CONFLICT
        }
    }

    enum ValidationResult {
        ACTIVE,
        REVOKED
    }
}
