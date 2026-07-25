package com.azurion.saascore.crm.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.azurion.saascore.crm.domain.entities.CrmLandingIngressRegistry;
import com.azurion.saascore.crm.domain.repositories.CrmLandingIngressRegistryRepository;
import com.azurion.shared.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CrmLandingIngressRegistryServiceTest {

    @Mock
    CrmLandingIngressRegistryRepository repository;
    @Mock
    CrmSecretEncryptionService encryptionService;

    CrmLandingIngressRegistryService service;

    @BeforeEach
    void setUp() {
        service = new CrmLandingIngressRegistryService(repository, encryptionService);
        ReflectionTestUtils.setField(service, "signatureToleranceSeconds", 300L);
    }

    @Test
    void acceptsValidServerSignature() throws Exception {
        String sourceKey = "lnd_source";
        String secret = "rls_super_secret";
        String body = "{\"nombre\":\"Juan\",\"telefono\":\"999999999\"}";
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        String idempotencyKey = "evt-123";
        CrmLandingIngressRegistry registry = registry(sourceKey);
        when(repository.findBySourceKeyAndActivoTrue(sourceKey)).thenReturn(Optional.of(registry));
        when(encryptionService.decrypt("encrypted")).thenReturn(secret);
        String signature = "sha256=" + sign(secret, timestamp + "." + idempotencyKey + "." + body);

        var resolved = service.verifyRelay(
                sourceKey,
                timestamp,
                idempotencyKey,
                body,
                signature
        );

        assertEquals("tenant_demo", resolved.tenantId());
    }

    @Test
    void rejectsInvalidServerSignature() {
        CrmLandingIngressRegistry registry = registry("lnd_source");
        when(repository.findBySourceKeyAndActivoTrue("lnd_source")).thenReturn(Optional.of(registry));
        when(encryptionService.decrypt("encrypted")).thenReturn("rls_super_secret");

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> service.verifyRelay(
                        "lnd_source",
                        Long.toString(Instant.now().getEpochSecond()),
                        "evt-123",
                        "{}",
                        "sha256=incorrecta"
                )
        );

        assertEquals("CRM_RELAY_FIRMA_INVALIDA", error.getCode());
    }

    private CrmLandingIngressRegistry registry(String sourceKey) {
        CrmLandingIngressRegistry registry = new CrmLandingIngressRegistry();
        registry.setSourceKey(sourceKey);
        registry.setTenantId("tenant_demo");
        registry.setLandingConfigId(12L);
        registry.setRelaySecretEncrypted("encrypted");
        registry.setActivo(true);
        return registry;
    }

    private String sign(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
