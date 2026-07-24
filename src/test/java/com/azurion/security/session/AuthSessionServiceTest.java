package com.azurion.security.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.azurion.security.jwt.JwtProperties;
import com.azurion.shared.exception.BusinessException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AuthSessionServiceTest {

    private final InMemorySessionStore store = new InMemorySessionStore();
    private final AuthSessionService service = service(store);

    @Test
    void firstLoginCreatesActiveSession() {
        AuthSessionRecord created = service.open("tenant-a", 9L, client("browser-a"));

        assertThat(created.state()).isEqualTo(AuthSessionRecord.ACTIVE);
        service.validate("tenant-a", 9L, created.sessionId());
    }

    @Test
    void sameBrowserReusesSessionAcrossTabs() {
        AuthSessionRecord first = service.open("tenant-a", 9L, client("browser-a"));
        AuthSessionRecord second = service.open("tenant-a", 9L, client("browser-a"));

        assertThat(second.sessionId()).isEqualTo(first.sessionId());
    }

    @Test
    void differentBrowserReturnsConflictWithoutChangingCurrentSession() {
        AuthSessionRecord first = service.open("tenant-a", 9L, client("browser-a"));

        assertThatThrownBy(() -> service.open("tenant-a", 9L, client("browser-b")))
                .isInstanceOf(ActiveSessionExistsException.class)
                .satisfies(error -> {
                    ActiveSessionExistsException conflict = (ActiveSessionExistsException) error;
                    assertThat(conflict.getActiveSession().sessionId()).isEqualTo(first.sessionId());
                    assertThat(conflict.getReplacementToken()).isNotBlank();
                });

        service.validate("tenant-a", 9L, first.sessionId());
    }

    @Test
    void confirmedReplacementRevokesPreviousSession() {
        AuthSessionRecord first = service.open("tenant-a", 9L, client("browser-a"));
        ActiveSessionExistsException conflict = conflictFor("tenant-a", 9L, "browser-b");
        ReplacementChallenge challenge =
                service.inspectReplacement(conflict.getReplacementToken());

        AuthSessionRecord replacement =
                service.replace(
                        conflict.getReplacementToken(),
                        challenge,
                        challenge.deviceId()
                );

        assertThat(replacement.sessionId()).isNotEqualTo(first.sessionId());
        assertThatThrownBy(() -> service.validate("tenant-a", 9L, first.sessionId()))
                .isInstanceOf(SessionRevokedException.class);
        service.validate("tenant-a", 9L, replacement.sessionId());
    }

    @Test
    void replacementTokenCanOnlyBeUsedOnce() {
        service.open("tenant-a", 9L, client("browser-a"));
        ActiveSessionExistsException conflict = conflictFor("tenant-a", 9L, "browser-b");
        ReplacementChallenge challenge =
                service.inspectReplacement(conflict.getReplacementToken());
        service.replace(
                conflict.getReplacementToken(),
                challenge,
                challenge.deviceId()
        );

        assertThatThrownBy(() -> service.inspectReplacement(conflict.getReplacementToken()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("vencio");
    }

    @Test
    void replacementChallengeCannotBeUsedByAnotherDevice() {
        service.open("tenant-a", 9L, client("browser-a"));
        ActiveSessionExistsException conflict = conflictFor("tenant-a", 9L, "browser-b");
        ReplacementChallenge challenge =
                service.inspectReplacement(conflict.getReplacementToken());

        assertThatThrownBy(() -> service.replace(
                conflict.getReplacementToken(),
                challenge,
                "browser-attacker"
        )).isInstanceOf(BusinessException.class);

        service.validate("tenant-a", 9L, challenge.previousSessionId());
    }

    @Test
    void expiredReplacementTokenIsRejected() {
        service.open("tenant-a", 9L, client("browser-a"));
        ActiveSessionExistsException conflict = conflictFor("tenant-a", 9L, "browser-b");
        store.expireReplacementTokens();

        assertThatThrownBy(() -> service.inspectReplacement(conflict.getReplacementToken()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void logoutOnlyRevokesCurrentSession() {
        AuthSessionRecord current = service.open("tenant-a", 9L, client("browser-a"));
        service.logout("tenant-a", 9L, current.sessionId(), client("browser-a"));

        assertThatThrownBy(() -> service.validate("tenant-a", 9L, current.sessionId()))
                .isInstanceOf(SessionRevokedException.class);
    }

    @Test
    void expiredSessionIsRejected() {
        AuthSessionRecord current = service.open("tenant-a", 9L, client("browser-a"));
        store.expireSession(current.sessionId());

        assertThatThrownBy(() -> service.validate("tenant-a", 9L, current.sessionId()))
                .isInstanceOf(SessionRevokedException.class);
    }

    @Test
    void sameUserIdIsIsolatedByTenant() {
        AuthSessionRecord tenantA = service.open("tenant-a", 9L, client("browser-a"));
        AuthSessionRecord tenantB = service.open("tenant-b", 9L, client("browser-b"));

        assertThat(tenantA.sessionId()).isNotEqualTo(tenantB.sessionId());
        service.validate("tenant-a", 9L, tenantA.sessionId());
        service.validate("tenant-b", 9L, tenantB.sessionId());
    }

    @Test
    void concurrentDifferentDevicesProduceOneWinner() throws Exception {
        int requests = 12;
        var executor = Executors.newFixedThreadPool(requests);
        var start = new CountDownLatch(1);
        var done = new CountDownLatch(requests);
        var created = new AtomicInteger();
        var conflicts = new AtomicInteger();
        var failures = new ArrayList<Throwable>();

        for (int index = 0; index < requests; index++) {
            int device = index;
            executor.submit(() -> {
                try {
                    start.await();
                    service.open("tenant-race", 77L, client("browser-" + device));
                    created.incrementAndGet();
                } catch (ActiveSessionExistsException ex) {
                    conflicts.incrementAndGet();
                } catch (Throwable ex) {
                    synchronized (failures) {
                        failures.add(ex);
                    }
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        executor.shutdownNow();
        assertThat(failures).isEmpty();
        assertThat(created).hasValue(1);
        assertThat(conflicts).hasValue(requests - 1);
    }

    @Test
    void redisFailureIsFailClosed() {
        AuthSessionStore unavailableStore = mock(AuthSessionStore.class);
        when(unavailableStore.reserve(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        )).thenThrow(new SessionStoreUnavailableException(new RuntimeException("offline")));
        AuthSessionService unavailableService = service(unavailableStore);

        assertThatThrownBy(() ->
                unavailableService.open("tenant-a", 9L, client("browser-a")))
                .isInstanceOf(SessionStoreUnavailableException.class);
    }

    private ActiveSessionExistsException conflictFor(
            String tenant,
            Long userId,
            String deviceId
    ) {
        try {
            service.open(tenant, userId, client(deviceId));
            throw new AssertionError("Se esperaba conflicto");
        } catch (ActiveSessionExistsException ex) {
            return ex;
        }
    }

    private SessionClientInfo client(String deviceId) {
        return new SessionClientInfo(
                deviceId,
                "Chrome en Windows",
                "127.0.0.1",
                "JUnit"
        );
    }

    private AuthSessionService service(AuthSessionStore sessionStore) {
        JwtProperties properties = new JwtProperties();
        properties.setExpirationMinutes(120);
        return new AuthSessionService(
                sessionStore,
                mock(AuthSessionAuditService.class),
                properties,
                Duration.ofMinutes(2),
                Duration.ofSeconds(30)
        );
    }

    private static final class InMemorySessionStore implements AuthSessionStore {

        private final Map<String, AuthSessionRecord> sessions = new ConcurrentHashMap<>();
        private final Map<String, String> active = new ConcurrentHashMap<>();
        private final Map<String, ReplacementChallenge> challenges = new ConcurrentHashMap<>();

        @Override
        public synchronized ReservationResult reserve(AuthSessionRecord candidate, Duration ttl) {
            String key = userKey(candidate.tenantId(), candidate.userId());
            String currentId = active.get(key);
            AuthSessionRecord current = currentId == null ? null : sessions.get(currentId);
            if (isActive(current)) {
                ReservationResult.Status status = current.deviceId().equals(candidate.deviceId())
                        ? ReservationResult.Status.SAME_DEVICE
                        : ReservationResult.Status.CONFLICT;
                return new ReservationResult(status, current.sessionId());
            }
            sessions.put(candidate.sessionId(), candidate);
            active.put(key, candidate.sessionId());
            return new ReservationResult(ReservationResult.Status.CREATED, candidate.sessionId());
        }

        @Override
        public Optional<AuthSessionRecord> find(String tenantId, Long userId, String sessionId) {
            AuthSessionRecord record = sessions.get(sessionId);
            return record != null
                    && tenantId.equals(record.tenantId())
                    && userId.equals(record.userId())
                    ? Optional.of(record)
                    : Optional.empty();
        }

        @Override
        public void saveReplacement(
                String tokenHash,
                ReplacementChallenge challenge,
                Duration ttl
        ) {
            challenges.put(tokenHash, challenge);
        }

        @Override
        public Optional<ReplacementChallenge> findReplacement(String tokenHash) {
            return Optional.ofNullable(challenges.get(tokenHash));
        }

        @Override
        public synchronized AuthSessionRecord replace(
                String tokenHash,
                ReplacementChallenge challenge,
                AuthSessionRecord candidate,
                Duration ttl
        ) {
            ReplacementChallenge stored = challenges.remove(tokenHash);
            String key = userKey(challenge.tenantId(), challenge.userId());
            if (stored == null || !challenge.previousSessionId().equals(active.get(key))) {
                throw new IllegalArgumentException("REPLACEMENT_TOKEN_INVALID");
            }
            revoke(challenge.tenantId(), challenge.userId(), challenge.previousSessionId(), Instant.now());
            sessions.put(candidate.sessionId(), candidate);
            active.put(key, candidate.sessionId());
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
            AuthSessionRecord record = sessions.get(sessionId);
            boolean valid = isActive(record)
                    && tenantId.equals(record.tenantId())
                    && userId.equals(record.userId())
                    && sessionId.equals(active.get(userKey(tenantId, userId)));
            return valid ? ValidationResult.ACTIVE : ValidationResult.REVOKED;
        }

        @Override
        public synchronized boolean revoke(
                String tenantId,
                Long userId,
                String sessionId,
                Instant revokedAt
        ) {
            AuthSessionRecord record = sessions.get(sessionId);
            if (record == null) {
                return false;
            }
            sessions.put(sessionId, withState(record, AuthSessionRecord.REVOKED, record.expiresAt()));
            active.remove(userKey(tenantId, userId), sessionId);
            return true;
        }

        void expireReplacementTokens() {
            challenges.clear();
        }

        void expireSession(String sessionId) {
            AuthSessionRecord record = sessions.get(sessionId);
            sessions.put(sessionId, withState(record, record.state(), Instant.now().minusSeconds(1)));
        }

        private boolean isActive(AuthSessionRecord record) {
            return record != null
                    && AuthSessionRecord.ACTIVE.equals(record.state())
                    && record.expiresAt().isAfter(Instant.now());
        }

        private AuthSessionRecord withState(
                AuthSessionRecord record,
                String state,
                Instant expiresAt
        ) {
            return new AuthSessionRecord(
                    record.sessionId(),
                    record.tenantId(),
                    record.userId(),
                    record.deviceId(),
                    record.deviceName(),
                    record.createdAt(),
                    record.lastActivityAt(),
                    expiresAt,
                    state
            );
        }

        private String userKey(String tenant, Long userId) {
            return tenant + ":" + userId;
        }
    }
}
