package com.azurion.security.session;

public record ReplacementChallenge(
        String tenantId,
        Long userId,
        String previousSessionId,
        String deviceId,
        String deviceName,
        String ipAddress,
        String userAgent
) {
}
