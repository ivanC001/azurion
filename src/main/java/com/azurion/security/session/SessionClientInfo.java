package com.azurion.security.session;

import jakarta.servlet.http.HttpServletRequest;

public record SessionClientInfo(
        String deviceId,
        String deviceName,
        String ipAddress,
        String userAgent
) {
    public static SessionClientInfo from(
            HttpServletRequest request,
            String deviceId,
            String deviceName
    ) {
        return new SessionClientInfo(
                deviceId,
                deviceName,
                clean(request.getRemoteAddr(), 80, "unknown"),
                clean(request.getHeader("User-Agent"), 300, "unknown")
        );
    }

    public static SessionClientInfo unknown(String deviceId, String deviceName) {
        return new SessionClientInfo(deviceId, deviceName, "unknown", "unknown");
    }

    private static String clean(String value, int maxLength, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String sanitized = value.replaceAll("[\\r\\n\\t]", " ").trim();
        return sanitized.substring(0, Math.min(maxLength, sanitized.length()));
    }
}
