package com.azurion.security.session;

import com.azurion.security.jwt.JwtProperties;
import com.azurion.shared.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthSessionService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AuthSessionStore store;
    private final AuthSessionAuditService audit;
    private final Duration sessionTtl;
    private final Duration replacementTtl;
    private final Duration touchInterval;

    public AuthSessionService(
            AuthSessionStore store,
            AuthSessionAuditService audit,
            JwtProperties jwtProperties,
            @Value("${azurion.security.sessions.replacement-token-ttl:2m}") Duration replacementTtl,
            @Value("${azurion.security.sessions.activity-touch-interval:30s}") Duration touchInterval
    ) {
        this.store = store;
        this.audit = audit;
        this.sessionTtl = jwtProperties.expiration();
        this.replacementTtl = replacementTtl;
        this.touchInterval = touchInterval;
    }

    public AuthSessionRecord open(
            String tenantId,
            Long userId,
            SessionClientInfo rawClient
    ) {
        SessionClientInfo client = normalized(rawClient);
        Instant now = Instant.now();
        AuthSessionRecord candidate = candidate(tenantId, userId, client, now);
        AuthSessionStore.ReservationResult result = store.reserve(candidate, sessionTtl);

        if (result.status() == AuthSessionStore.ReservationResult.Status.CREATED) {
            audit.record("LOGIN_SUCCESS", tenantId, userId, candidate.sessionId(), client, 200);
            return candidate;
        }

        AuthSessionRecord existing = store.find(tenantId, userId, result.sessionId())
                .orElseThrow(() -> new SessionRevokedException());
        if (result.status() == AuthSessionStore.ReservationResult.Status.SAME_DEVICE) {
            audit.record("LOGIN_SUCCESS", tenantId, userId, existing.sessionId(), client, 200);
            return existing;
        }

        String replacementToken = randomToken();
        store.saveReplacement(
                hash(replacementToken),
                new ReplacementChallenge(
                        tenantId,
                        userId,
                        existing.sessionId(),
                        client.deviceId(),
                        client.deviceName(),
                        client.ipAddress(),
                        client.userAgent()
                ),
                replacementTtl
        );
        audit.record(
                "LOGIN_ACTIVE_SESSION_CONFLICT",
                tenantId,
                userId,
                existing.sessionId(),
                client,
                409
        );
        throw new ActiveSessionExistsException(existing, replacementToken);
    }

    public ReplacementChallenge inspectReplacement(String replacementToken) {
        return store.findReplacement(hashRequired(replacementToken))
                .orElseThrow(this::invalidReplacement);
    }

    public AuthSessionRecord replace(
            String replacementToken,
            ReplacementChallenge challenge,
            String deviceId
    ) {
        if (deviceId == null || !challenge.deviceId().equals(deviceId.trim())) {
            throw invalidReplacement();
        }
        SessionClientInfo client = new SessionClientInfo(
                challenge.deviceId(),
                challenge.deviceName(),
                challenge.ipAddress(),
                challenge.userAgent()
        );
        Instant now = Instant.now();
        AuthSessionRecord candidate = candidate(challenge.tenantId(), challenge.userId(), client, now);
        try {
            AuthSessionRecord created = store.replace(
                    hashRequired(replacementToken),
                    challenge,
                    candidate,
                    sessionTtl
            );
            audit.record(
                    "SESSION_REPLACED",
                    challenge.tenantId(),
                    challenge.userId(),
                    created.sessionId(),
                    client,
                    200
            );
            audit.record(
                    "SESSION_REVOKED",
                    challenge.tenantId(),
                    challenge.userId(),
                    challenge.previousSessionId(),
                    client,
                    401
            );
            return created;
        } catch (IllegalArgumentException ex) {
            throw invalidReplacement();
        }
    }

    public void validate(String tenantId, Long userId, String sessionId) {
        validate(
                tenantId,
                userId,
                sessionId,
                SessionClientInfo.unknown("unknown", "Navegador desconocido")
        );
    }

    public void validate(
            String tenantId,
            Long userId,
            String sessionId,
            SessionClientInfo client
    ) {
        if (sessionId == null || sessionId.isBlank()) {
            audit.record("SESSION_VALIDATION_FAILED", tenantId, userId, sessionId, client, 401);
            throw new SessionRevokedException();
        }
        AuthSessionStore.ValidationResult result;
        try {
            result = store.validateAndTouch(
                    tenantId,
                    userId,
                    sessionId,
                    Instant.now(),
                    touchInterval
            );
        } catch (SessionStoreUnavailableException ex) {
            audit.record("SESSION_VALIDATION_FAILED", tenantId, userId, sessionId, client, 503);
            throw ex;
        }
        if (result != AuthSessionStore.ValidationResult.ACTIVE) {
            audit.record("SESSION_VALIDATION_FAILED", tenantId, userId, sessionId, client, 401);
            audit.record("SESSION_REVOKED", tenantId, userId, sessionId, client, 401);
            throw new SessionRevokedException();
        }
    }

    public void logout(
            String tenantId,
            Long userId,
            String sessionId,
            SessionClientInfo client
    ) {
        boolean revoked = store.revoke(tenantId, userId, sessionId, Instant.now());
        if (revoked) {
            audit.record("LOGOUT", tenantId, userId, sessionId, normalized(client), 200);
        }
    }

    private AuthSessionRecord candidate(
            String tenantId,
            Long userId,
            SessionClientInfo client,
            Instant now
    ) {
        return new AuthSessionRecord(
                UUID.randomUUID().toString(),
                tenantId,
                userId,
                client.deviceId(),
                client.deviceName(),
                now,
                now,
                now.plus(sessionTtl),
                AuthSessionRecord.ACTIVE
        );
    }

    private SessionClientInfo normalized(SessionClientInfo client) {
        if (client == null || client.deviceId() == null || client.deviceId().isBlank()) {
            throw new BusinessException(
                    "DEVICE_ID_REQUERIDO",
                    "No se pudo identificar este navegador. Recarga la pagina e intenta nuevamente."
            );
        }
        String deviceId = clean(client.deviceId(), 120, null);
        String deviceName = clean(client.deviceName(), 120, "Navegador desconocido");
        return new SessionClientInfo(
                deviceId,
                deviceName,
                clean(client.ipAddress(), 80, "unknown"),
                clean(client.userAgent(), 300, "unknown")
        );
    }

    private String clean(String value, int maxLength, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String result = value.replaceAll("[\\r\\n\\t]", " ").trim();
        return result.substring(0, Math.min(maxLength, result.length()));
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashRequired(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw invalidReplacement();
        }
        return hash(rawToken);
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    private BusinessException invalidReplacement() {
        return new BusinessException(
                "REPLACEMENT_TOKEN_INVALID",
                "La autorizacion para reemplazar la sesion vencio o ya fue utilizada."
        );
    }
}
