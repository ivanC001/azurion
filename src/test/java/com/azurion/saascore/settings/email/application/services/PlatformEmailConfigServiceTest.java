package com.azurion.saascore.settings.email.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.saascore.settings.email.application.dto.PlatformEmailConfigRequest;
import com.azurion.saascore.settings.email.application.dto.PlatformEmailConfigResponse;
import com.azurion.saascore.settings.email.domain.entities.PlatformEmailConfig;
import com.azurion.saascore.settings.email.domain.entities.SmtpSecurity;
import com.azurion.saascore.settings.email.domain.entities.TenantEmailConfigStatus;
import com.azurion.saascore.settings.email.domain.repositories.PlatformEmailConfigRepository;
import com.azurion.shared.exception.BusinessException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

class PlatformEmailConfigServiceTest {

    private final PlatformEmailConfigRepository repository = mock(PlatformEmailConfigRepository.class);
    private final EmailSecretEncryptionService encryptionService = mock(EmailSecretEncryptionService.class);
    private final SmtpEmailTransportService smtpTransport = mock(SmtpEmailTransportService.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final PlatformEmailConfigService service = new PlatformEmailConfigService(
            repository,
            encryptionService,
            smtpTransport,
            transactionManager
    );

    @BeforeEach
    void persistAssignedIds() {
        when(repository.save(any(PlatformEmailConfig.class))).thenAnswer(invocation -> {
            PlatformEmailConfig config = invocation.getArgument(0);
            if (config.getId() == null) {
                config.setId(1L);
            }
            return config;
        });
    }

    @Test
    void requiresPasswordWhenCreatingConfiguration() {
        when(repository.findByConfigKey(PlatformEmailConfigService.CONFIG_KEY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.saveOrUpdate(request(null, true, true, true)))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getCode())
                .isEqualTo("PLATFORM_EMAIL_SMTP_PASSWORD_REQUIRED");
    }

    @Test
    void encryptsPasswordAndInvalidatesVerificationAfterSmtpChange() {
        PlatformEmailConfig existing = verifiedConfig();
        when(repository.findByConfigKey(PlatformEmailConfigService.CONFIG_KEY))
                .thenReturn(Optional.of(existing));
        when(encryptionService.encrypt("new-app-password")).thenReturn("encrypted-value");

        PlatformEmailConfigResponse response = service.saveOrUpdate(
                new PlatformEmailConfigRequest(
                        "Azurion",
                        "avisos@azurion.com",
                        "soporte@azurion.com",
                        "smtp.changed.test",
                        587,
                        SmtpSecurity.TLS,
                        "avisos@azurion.com",
                        "new-app-password",
                        true,
                        true,
                        false,
                        true
                )
        );

        verify(encryptionService).encrypt("new-app-password");
        assertThat(existing.getSmtpPasswordEncrypted()).isEqualTo("encrypted-value");
        assertThat(response.verificado()).isFalse();
        assertThat(response.activo()).isFalse();
        assertThat(response.estado()).isEqualTo(TenantEmailConfigStatus.PENDIENTE);
        assertThat(response.reportesHabilitados()).isFalse();
        assertThat(response.smtpPasswordConfigured()).isTrue();
    }

    @Test
    void refusesDeliveryWhenRequestedPurposeWasDisabled() {
        PlatformEmailConfig existing = verifiedConfig();
        existing.setDobleFactorHabilitado(false);
        when(repository.findByConfigKey(PlatformEmailConfigService.CONFIG_KEY))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.getVerifiedConfigOrThrow(PlatformEmailPurpose.TWO_FACTOR))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getCode())
                .isEqualTo("PLATFORM_EMAIL_PURPOSE_DISABLED");
    }

    private PlatformEmailConfigRequest request(
            String password,
            boolean notifications,
            boolean reports,
            boolean twoFactor
    ) {
        return new PlatformEmailConfigRequest(
                "Azurion",
                "avisos@azurion.com",
                null,
                "smtp.azurion.com",
                587,
                SmtpSecurity.TLS,
                "avisos@azurion.com",
                password,
                true,
                notifications,
                reports,
                twoFactor
        );
    }

    private PlatformEmailConfig verifiedConfig() {
        PlatformEmailConfig config = new PlatformEmailConfig();
        config.setId(1L);
        config.setConfigKey(PlatformEmailConfigService.CONFIG_KEY);
        config.setNombreRemitente("Azurion");
        config.setCorreoRemitente("avisos@azurion.com");
        config.setSmtpHost("smtp.azurion.com");
        config.setSmtpPort(587);
        config.setSmtpSecurity(SmtpSecurity.TLS);
        config.setSmtpUsername("avisos@azurion.com");
        config.setSmtpPasswordEncrypted("encrypted");
        config.setActivo(true);
        config.setVerificado(true);
        config.setEstado(TenantEmailConfigStatus.VERIFICADO);
        config.setAvisosHabilitados(true);
        config.setReportesHabilitados(true);
        config.setDobleFactorHabilitado(true);
        return config;
    }
}
