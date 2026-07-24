package com.azurion.security.session;

import java.time.Instant;

public record AuthSessionRecord(
        String sessionId,
        String tenantId,
        Long userId,
        String deviceId,
        String deviceName,
        Instant createdAt,
        Instant lastActivityAt,
        Instant expiresAt,
        String state
) {
    public static final String ACTIVE = "ACTIVE";
    public static final String REVOKED = "REVOKED";
}
