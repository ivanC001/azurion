package com.azurion.shared.util;

public final class StringUtil {

    private StringUtil() {
    }

    public static String trimToMax(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    public static String trimHeaderValue(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String safe = value.replace("\r", "").replace("\n", "").trim();
        return safe.length() <= maxLength ? safe : safe.substring(0, maxLength);
    }

    public static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String candidate : values) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate.trim();
            }
        }
        return null;
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
