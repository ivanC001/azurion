package com.azurion.saascore.facturacion.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FacturadorPropertiesTest {

    @Test
    void prefersClientCredentialsAndKeepsSecretOutOfClientId() {
        FacturadorProperties properties = new FacturadorProperties();
        properties.setClientId("azurion-production");
        properties.setClientSecret("a-secret-that-must-not-be-sent");
        properties.setDefaultApiKey("legacy-key");

        var credential = properties.resolveCredential("tenant-a").orElseThrow();

        assertEquals("azurion-production", credential.clientId());
        assertEquals("a-secret-that-must-not-be-sent", credential.secret());
        assertEquals("v2", credential.signatureVersion());
        assertTrue(!credential.legacyApiKey());
    }

    @Test
    void legacyCredentialRequiresExplicitOptIn() {
        FacturadorProperties properties = new FacturadorProperties();
        properties.setDefaultApiKey("legacy-key");

        assertTrue(properties.resolveCredential("tenant-a").isEmpty());

        properties.setAllowLegacyApiKey(true);
        assertTrue(properties.resolveCredential("tenant-a").orElseThrow().legacyApiKey());
    }
}
