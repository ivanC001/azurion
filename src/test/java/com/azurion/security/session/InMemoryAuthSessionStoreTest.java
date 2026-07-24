package com.azurion.security.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryAuthSessionStoreTest {

    private InMemoryAuthSessionStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryAuthSessionStore();
    }

    @Test
    void reservesOneActiveSessionPerTenantUser() {
        AuthSessionRecord first = session("session-1", "device-1");
        AuthSessionRecord sameDevice = session("session-2", "device-1");
        AuthSessionRecord otherDevice = session("session-3", "device-2");

        assertThat(store.reserve(first, Duration.ofHours(2)).status())
                .isEqualTo(AuthSessionStore.ReservationResult.Status.CREATED);
        assertThat(store.reserve(sameDevice, Duration.ofHours(2)))
                .isEqualTo(new AuthSessionStore.ReservationResult(
                        AuthSessionStore.ReservationResult.Status.SAME_DEVICE,
                        "session-1"
                ));
        assertThat(store.reserve(otherDevice, Duration.ofHours(2)))
                .isEqualTo(new AuthSessionStore.ReservationResult(
                        AuthSessionStore.ReservationResult.Status.CONFLICT,
                        "session-1"
                ));
    }

    @Test
    void replacementRevokesPreviousSessionAndConsumesChallenge() {
        AuthSessionRecord previous = session("session-1", "device-1");
        AuthSessionRecord replacement = session("session-2", "device-2");
        ReplacementChallenge challenge = new ReplacementChallenge(
                "tenant-a",
                7L,
                previous.sessionId(),
                previous.deviceId(),
                previous.deviceName(),
                "127.0.0.1",
                "test"
        );

        store.reserve(previous, Duration.ofHours(2));
        store.saveReplacement("token-hash", challenge, Duration.ofMinutes(2));

        assertThat(store.replace("token-hash", challenge, replacement, Duration.ofHours(2)))
                .isEqualTo(replacement);
        assertThat(store.validateAndTouch(
                "tenant-a",
                7L,
                previous.sessionId(),
                Instant.now(),
                Duration.ofSeconds(30)
        )).isEqualTo(AuthSessionStore.ValidationResult.REVOKED);
        assertThat(store.validateAndTouch(
                "tenant-a",
                7L,
                replacement.sessionId(),
                Instant.now(),
                Duration.ofSeconds(30)
        )).isEqualTo(AuthSessionStore.ValidationResult.ACTIVE);
        assertThat(store.findReplacement("token-hash")).isEmpty();
    }

    @Test
    void rejectsMissingReplacementChallenge() {
        ReplacementChallenge challenge = new ReplacementChallenge(
                "tenant-a",
                7L,
                "session-1",
                "device-1",
                "Equipo",
                "127.0.0.1",
                "test"
        );

        assertThatThrownBy(() -> store.replace(
                "missing",
                challenge,
                session("session-2", "device-2"),
                Duration.ofHours(2)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("REPLACEMENT_TOKEN_INVALID");
    }

    @Test
    void revokesOnlyTheOwnedSession() {
        AuthSessionRecord session = session("session-1", "device-1");
        store.reserve(session, Duration.ofHours(2));

        assertThat(store.revoke("tenant-a", 999L, session.sessionId(), Instant.now())).isFalse();
        assertThat(store.revoke("tenant-a", 7L, session.sessionId(), Instant.now())).isTrue();
        assertThat(store.validateAndTouch(
                "tenant-a",
                7L,
                session.sessionId(),
                Instant.now(),
                Duration.ofSeconds(30)
        )).isEqualTo(AuthSessionStore.ValidationResult.REVOKED);
    }

    private AuthSessionRecord session(String sessionId, String deviceId) {
        Instant now = Instant.now();
        return new AuthSessionRecord(
                sessionId,
                "tenant-a",
                7L,
                deviceId,
                "Equipo",
                now,
                now,
                now.plus(Duration.ofHours(2)),
                AuthSessionRecord.ACTIVE
        );
    }
}
