package com.azurion.shared.util;

import com.fasterxml.jackson.databind.JsonNode;

public final class JsonNodeValues {

    private static final int DEFAULT_MAX_LENGTH = 500;

    private JsonNodeValues() {
    }

    public static String text(JsonNode source, String... keys) {
        JsonNode node = find(source, keys);
        if (node == null) {
            return null;
        }
        return trimToMax(node.asText(null), DEFAULT_MAX_LENGTH);
    }

    public static String url(JsonNode source, String... keys) {
        String value = text(source, keys);
        if (value == null) {
            return null;
        }
        if (value.regionMatches(true, 0, "http://", 0, 7)
                || value.regionMatches(true, 0, "https://", 0, 8)) {
            return value;
        }
        return null;
    }

    public static JsonNode find(JsonNode source, String... keys) {
        if (source == null || source.isNull() || source.isMissingNode()) {
            return null;
        }

        for (String key : keys) {
            JsonNode value = source.get(key);
            if (hasValue(value)) {
                return value;
            }
        }

        for (JsonNode child : source) {
            JsonNode nested = find(child, keys);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    private static boolean hasValue(JsonNode value) {
        return value != null
                && !value.isNull()
                && !value.isMissingNode()
                && !value.asText("").isBlank();
    }

    private static String trimToMax(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
