package com.azurion.saascore.crm.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.saascore.crm.application.dto.WhatsappConnectionStatusResponse;
import com.azurion.saascore.crm.application.dto.WhatsappVerifyTokenResponse;
import com.azurion.saascore.crm.domain.entities.CrmCanalTokenConfig;
import com.azurion.saascore.crm.domain.repositories.CrmCanalTokenConfigRepository;
import com.azurion.saascore.crm.infrastructure.http.WhatsappCloudApiClient;
import com.azurion.saascore.crm.infrastructure.http.WhatsappCloudApiClient.ConnectionCheck;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WhatsappConfigurationServiceTest {

    @Mock
    private CrmCanalTokenConfigRepository configRepository;
    @Mock
    private CrmSecretEncryptionService secretEncryptionService;
    @Mock
    private WhatsappCloudApiClient cloudApiClient;

    private WhatsappConfigurationService service;
    private CrmCanalTokenConfig config;

    @BeforeEach
    void setUp() {
        service = new WhatsappConfigurationService(configRepository, secretEncryptionService, cloudApiClient);
        config = new CrmCanalTokenConfig();
        config.setCanal("WHATSAPP");
        config.setNombre("WhatsApp Business");
    }

    @Test
    void generatesAndStoresAnOpaqueVerifyToken() {
        config.setWebhookVerifiedAt(OffsetDateTime.now(ZoneOffset.UTC));
        when(configRepository.findByCanal("WHATSAPP")).thenReturn(Optional.of(config));
        when(secretEncryptionService.encrypt(anyString())).thenReturn("encrypted-token");

        WhatsappVerifyTokenResponse response = service.generateVerifyToken();

        assertEquals(43, response.verifyToken().length());
        assertFalse(response.verifyToken().contains("="));
        assertEquals("encrypted-token", config.getVerifyToken());
        assertNull(config.getWebhookVerifiedAt());
        verify(secretEncryptionService).encrypt(response.verifyToken());
        verify(configRepository).save(config);
    }

    @Test
    void reportsConnectedOnlyAfterMetaAndWebhookChecksPass() {
        config.setActivo(true);
        config.setAccessToken("encrypted-access");
        config.setVerifyToken("encrypted-verify");
        config.setAppId("123");
        config.setAppSecret("encrypted-secret");
        config.setWabaId("456");
        config.setPhoneNumberId("789");
        config.setWebhookVerifiedAt(OffsetDateTime.now(ZoneOffset.UTC));
        OffsetDateTime checkedAt = OffsetDateTime.now(ZoneOffset.UTC);
        when(configRepository.findByCanal("WHATSAPP")).thenReturn(Optional.of(config));
        when(cloudApiClient.testConnection(config)).thenReturn(new ConnectionCheck(
                true,
                true,
                "+51 999 888 777",
                "Azurion",
                "GREEN",
                null,
                List.of("whatsapp_business_messaging", "whatsapp_business_management"),
                "Credenciales validas y aplicacion suscrita al WABA",
                checkedAt
        ));

        WhatsappConnectionStatusResponse response = service.testConnection();

        assertTrue(response.configuracionCompleta());
        assertTrue(response.accesoMetaValido());
        assertTrue(response.wabaSuscrita());
        assertTrue(response.webhookVerificado());
        assertTrue(response.conectado());
        assertEquals("+51 999 888 777", response.displayPhoneNumber());
        verify(configRepository).save(config);
    }

    @Test
    void subscribesTheAppAndRefreshesTheConnectionStatus() {
        config.setActivo(true);
        config.setAccessToken("encrypted-access");
        config.setVerifyToken("encrypted-verify");
        config.setAppId("123");
        config.setAppSecret("encrypted-secret");
        config.setWabaId("456");
        config.setPhoneNumberId("789");
        config.setWebhookVerifiedAt(OffsetDateTime.now(ZoneOffset.UTC));
        OffsetDateTime checkedAt = OffsetDateTime.now(ZoneOffset.UTC);
        when(configRepository.findByCanal("WHATSAPP")).thenReturn(Optional.of(config));
        when(cloudApiClient.testConnection(config)).thenReturn(new ConnectionCheck(
                true,
                true,
                "+51 999 888 777",
                "Azurion",
                "GREEN",
                null,
                List.of("whatsapp_business_messaging", "whatsapp_business_management"),
                "Credenciales validas y aplicacion suscrita al WABA",
                checkedAt
        ));

        WhatsappConnectionStatusResponse response = service.subscribeApp();

        verify(cloudApiClient).subscribeApp(config);
        verify(cloudApiClient).testConnection(config);
        verify(configRepository).save(config);
        assertTrue(response.wabaSuscrita());
        assertTrue(response.conectado());
    }

    @Test
    void stopsReportingConnectedWhenTheMetaTokenExpires() {
        config.setActivo(true);
        config.setAccessToken("encrypted-access");
        config.setVerifyToken("encrypted-verify");
        config.setAppId("123");
        config.setAppSecret("encrypted-secret");
        config.setWabaId("456");
        config.setPhoneNumberId("789");
        config.setWebhookVerifiedAt(OffsetDateTime.now(ZoneOffset.UTC));
        config.setLastConnectionOk(true);
        config.setWabaSubscribed(true);
        config.setMetaTokenExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));
        when(configRepository.findByCanal("WHATSAPP")).thenReturn(Optional.of(config));

        WhatsappConnectionStatusResponse response = service.getStatus();

        assertFalse(response.accesoMetaValido());
        assertFalse(response.conectado());
        assertEquals("El Access token de Meta esta vencido", response.message());
    }
}
