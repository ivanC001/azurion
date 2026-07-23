package com.azurion.saascore.crm.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.saascore.crm.domain.entities.CrmCanalTokenConfig;
import com.azurion.saascore.crm.domain.repositories.CrmCanalTokenConfigRepository;
import com.azurion.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetaWebhookServiceTest {

    private static final String APP_SECRET = "meta-app-secret";

    @Mock
    private CrmCanalTokenConfigRepository configRepository;
    @Mock
    private CrmSecretEncryptionService secretEncryptionService;

    private MetaWebhookService service;
    private CrmCanalTokenConfig config;

    @BeforeEach
    void setUp() {
        service = new MetaWebhookService(configRepository, secretEncryptionService, new ObjectMapper());
        config = new CrmCanalTokenConfig();
        config.setCanal("FACEBOOK");
        config.setNombre("Facebook Lead Ads");
        config.setActivo(true);
        config.setVerifyToken("encrypted-verify-token");
        config.setAppSecret("encrypted-app-secret");
        when(configRepository.findByCanal("FACEBOOK")).thenReturn(Optional.of(config));
    }

    @Test
    void verifiesChallengeUsingTenantVerifyToken() {
        when(secretEncryptionService.decrypt("encrypted-verify-token")).thenReturn("verify-azurion");

        String challenge = service.verify("facebook", "subscribe", "verify-azurion", "123456");

        assertEquals("123456", challenge);
        assertNotNull(config.getWebhookVerifiedAt());
        verify(configRepository).save(config);
    }

    @Test
    void acceptsSignedFacebookWebhook() throws Exception {
        String payload = "{\"object\":\"page\",\"entry\":[{\"id\":\"page-1\"}]}";
        when(secretEncryptionService.decrypt("encrypted-app-secret")).thenReturn(APP_SECRET);

        var result = service.receive("FACEBOOK", payload, signature(payload));

        assertEquals("FACEBOOK", result.canal());
        assertEquals(1, result.eventosRecibidos());
        assertNotNull(config.getLastWebhookAt());
        verify(configRepository).save(config);
    }

    @Test
    void rejectsWebhookWithInvalidSignature() {
        when(secretEncryptionService.decrypt("encrypted-app-secret")).thenReturn(APP_SECRET);

        assertThrows(BusinessException.class, () -> service.receive(
                "FACEBOOK",
                "{\"object\":\"page\",\"entry\":[]}",
                "sha256=invalid"
        ));
    }

    private String signature(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(APP_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
