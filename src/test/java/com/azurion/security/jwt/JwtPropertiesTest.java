package com.azurion.security.jwt;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

    @Test
    void acceptsStrongConfiguration() {
        JwtProperties properties = properties(
                "a-secure-random-jwt-secret-with-more-than-32-bytes",
                "azurion",
                30
        );

        assertThatCode(properties::validate).doesNotThrowAnyException();
    }

    @Test
    void rejectsKnownPlaceholderAndShortSecrets() {
        assertThatThrownBy(() -> properties("change-me", "azurion", 30).validate())
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> properties("too-short", "azurion", 30).validate())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsInvalidIssuerAndExpiration() {
        String secret = "a-secure-random-jwt-secret-with-more-than-32-bytes";

        assertThatThrownBy(() -> properties(secret, " ", 30).validate())
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> properties(secret, "azurion", 0).validate())
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> properties(secret, "azurion", 10_081).validate())
                .isInstanceOf(IllegalStateException.class);
    }

    private JwtProperties properties(String secret, String issuer, long expirationMinutes) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(secret);
        properties.setIssuer(issuer);
        properties.setExpirationMinutes(expirationMinutes);
        return properties;
    }
}
