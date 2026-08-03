package com.azurion.shared.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class JsonNodeValuesTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void readsFirstNonBlankValueFromNestedObjectsAndArrays() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {"data":[{"pdf_url":""},{"result":{"pdf_url":" https://files.test/document.pdf "}}]}
                """);

        assertThat(JsonNodeValues.text(payload, "pdf_url")).isEqualTo("https://files.test/document.pdf");
        assertThat(JsonNodeValues.url(payload, "pdf_url")).isEqualTo("https://files.test/document.pdf");
    }

    @Test
    void rejectsNonHttpUrlsAndLimitsTextLength() throws Exception {
        JsonNode payload = objectMapper.createObjectNode()
                .put("pdf_url", "file:///tmp/document.pdf")
                .put("message", "x".repeat(600));

        assertThat(JsonNodeValues.url(payload, "pdf_url")).isNull();
        assertThat(JsonNodeValues.text(payload, "message")).hasSize(500);
    }
}
