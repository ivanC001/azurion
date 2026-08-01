package com.azurion.shared.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class RequestFingerprint {

    private RequestFingerprint() {
    }

    public static String sha256(Object... values) {
        StringBuilder canonical = new StringBuilder();
        for (Object rawValue : values) {
            String value = rawValue == null ? "" : rawValue.toString();
            canonical.append(value.length()).append(':').append(value).append('|');
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(canonical.toString().getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no esta disponible", exception);
        }
    }
}
