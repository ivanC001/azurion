package com.azurion.saascore.facturacion.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class FacturadorHmacSignerTest {

    private final FacturadorHmacSigner signer = new FacturadorHmacSigner();

    @Test
    void v2IncludesTenantIdentityInCanonicalPayload() {
        String base = signer.buildCanonical(
                "v2", "POST", "/api/tickets", "1770000000", "nonce-12345678",
                "tenant-a", "20111111111", "body-hash"
        );
        String anotherTenant = signer.buildCanonical(
                "v2", "POST", "/api/tickets", "1770000000", "nonce-12345678",
                "tenant-b", "20111111111", "body-hash"
        );

        assertNotEquals(base, anotherTenant);
        assertEquals(
                "v2\nPOST\n/api/tickets\n1770000000\nnonce-12345678\ntenant-a\n20111111111\nbody-hash",
                base
        );
    }

    @Test
    void v1KeepsLegacyCanonicalPayload() {
        assertEquals(
                "POST\n/api/tickets\n1770000000\nnonce-12345678\nbody-hash",
                signer.buildCanonical(
                        "v1", "POST", "/api/tickets", "1770000000", "nonce-12345678",
                        "tenant-a", "20111111111", "body-hash"
                )
        );
    }
}
