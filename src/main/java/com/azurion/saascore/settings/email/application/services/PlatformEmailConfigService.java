package com.azurion.saascore.settings.email.application.services;

import com.azurion.saascore.settings.email.application.dto.PlatformEmailConfigRequest;
import com.azurion.saascore.settings.email.application.dto.PlatformEmailConfigResponse;
import com.azurion.saascore.settings.email.application.dto.TestEmailRequest;
import com.azurion.saascore.settings.email.domain.entities.PlatformEmailConfig;
import com.azurion.saascore.settings.email.domain.entities.TenantEmailConfigStatus;
import com.azurion.saascore.settings.email.domain.repositories.PlatformEmailConfigRepository;
import com.azurion.shared.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class PlatformEmailConfigService {

    static final String CONFIG_KEY = "AZURION_GLOBAL";

    private final PlatformEmailConfigRepository repository;
    private final EmailSecretEncryptionService encryptionService;
    private final SmtpEmailTransportService smtpTransport;
    private final PlatformTransactionManager transactionManager;

    @Transactional(readOnly = true)
    public PlatformEmailConfigResponse getConfig() {
        return repository.findByConfigKey(CONFIG_KEY)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional
    public PlatformEmailConfigResponse saveOrUpdate(PlatformEmailConfigRequest request) {
        PlatformEmailConfig config = repository.findByConfigKey(CONFIG_KEY).orElseGet(() -> {
            PlatformEmailConfig item = new PlatformEmailConfig();
            item.setConfigKey(CONFIG_KEY);
            item.setEstado(TenantEmailConfigStatus.PENDIENTE);
            return item;
        });
        boolean creating = config.getId() == null;
        if (creating && isBlank(request.smtpPassword())) {
            throw new BusinessException(
                    "PLATFORM_EMAIL_SMTP_PASSWORD_REQUIRED",
                    "La contrasena SMTP es obligatoria al crear la configuracion."
            );
        }

        String newEncryptedPassword = isBlank(request.smtpPassword())
                ? null
                : encryptionService.encrypt(request.smtpPassword().trim());
        boolean smtpChanged = creating
                || changed(config.getSmtpHost(), request.smtpHost())
                || !Objects.equals(config.getSmtpPort(), request.smtpPort())
                || config.getSmtpSecurity() != request.smtpSecurity()
                || changed(config.getSmtpUsername(), request.smtpUsername())
                || changed(config.getCorreoRemitente(), request.correoRemitente())
                || newEncryptedPassword != null;

        config.setNombreRemitente(trim(request.nombreRemitente()));
        config.setCorreoRemitente(trim(request.correoRemitente()));
        config.setReplyTo(emptyToNull(request.replyTo()));
        config.setSmtpHost(trim(request.smtpHost()));
        config.setSmtpPort(request.smtpPort());
        config.setSmtpSecurity(request.smtpSecurity());
        config.setSmtpUsername(trim(request.smtpUsername()));
        if (newEncryptedPassword != null) {
            config.setSmtpPasswordEncrypted(newEncryptedPassword);
        }
        config.setAvisosHabilitados(!Boolean.FALSE.equals(request.avisosHabilitados()));
        config.setReportesHabilitados(!Boolean.FALSE.equals(request.reportesHabilitados()));
        config.setDobleFactorHabilitado(!Boolean.FALSE.equals(request.dobleFactorHabilitado()));

        if (smtpChanged) {
            config.setActivo(false);
            config.setVerificado(false);
            config.setEstado(TenantEmailConfigStatus.PENDIENTE);
            config.setFechaVerificacion(null);
            config.setUltimoError(null);
        } else if (Boolean.TRUE.equals(request.activo()) && config.isVerificado()) {
            config.setActivo(true);
            config.setEstado(TenantEmailConfigStatus.VERIFICADO);
        } else if (!Boolean.TRUE.equals(request.activo())) {
            config.setActivo(false);
            config.setEstado(config.isVerificado()
                    ? TenantEmailConfigStatus.INACTIVO
                    : TenantEmailConfigStatus.PENDIENTE);
        }

        return toResponse(repository.save(config));
    }

    public PlatformEmailConfigResponse test(TestEmailRequest request) {
        PlatformEmailConfig config = inTransaction(() -> repository.findByConfigKey(CONFIG_KEY)
                .orElseThrow(() -> new BusinessException(
                        "PLATFORM_EMAIL_CONFIG_NOT_FOUND",
                        "Configura y guarda el correo de Azurion antes de enviar la prueba."
                )));
        try {
            smtpTransport.send(
                    config,
                    trim(request.correoDestino()),
                    "Correo de prueba de la plataforma Azurion",
                    "La configuracion SMTP global de Azurion fue verificada correctamente.",
                    List.of()
            );
            return inTransaction(() -> updateVerification(config.getId(), true, null));
        } catch (BusinessException ex) {
            inTransaction(() -> updateVerification(config.getId(), false, ex.getMessage()));
            throw ex;
        }
    }

    @Transactional
    public PlatformEmailConfigResponse activate() {
        PlatformEmailConfig config = findOrThrow();
        if (!config.isVerificado()) {
            throw new BusinessException(
                    "PLATFORM_EMAIL_CONFIG_NOT_VERIFIED",
                    "Envia un correo de prueba correctamente antes de activar el correo de Azurion."
            );
        }
        config.setActivo(true);
        config.setEstado(TenantEmailConfigStatus.VERIFICADO);
        return toResponse(repository.save(config));
    }

    @Transactional
    public PlatformEmailConfigResponse deactivate() {
        PlatformEmailConfig config = findOrThrow();
        config.setActivo(false);
        config.setEstado(TenantEmailConfigStatus.INACTIVO);
        return toResponse(repository.save(config));
    }

    @Transactional(readOnly = true)
    public PlatformEmailConfig getVerifiedConfigOrThrow(PlatformEmailPurpose purpose) {
        PlatformEmailConfig config = findOrThrow();
        if (!config.isActivo()
                || !config.isVerificado()
                || config.getEstado() != TenantEmailConfigStatus.VERIFICADO) {
            throw new BusinessException(
                    "PLATFORM_EMAIL_CONFIG_NOT_ACTIVE",
                    "El correo global de Azurion no esta activo y verificado."
            );
        }
        boolean enabled = switch (purpose) {
            case NOTIFICATION -> config.isAvisosHabilitados();
            case REPORT -> config.isReportesHabilitados();
            case TWO_FACTOR -> config.isDobleFactorHabilitado();
        };
        if (!enabled) {
            throw new BusinessException(
                    "PLATFORM_EMAIL_PURPOSE_DISABLED",
                    "El tipo de correo solicitado esta deshabilitado en la configuracion de Azurion."
            );
        }
        return config;
    }

    PlatformEmailConfigResponse toResponse(PlatformEmailConfig config) {
        return new PlatformEmailConfigResponse(
                config.getId(),
                config.getNombreRemitente(),
                config.getCorreoRemitente(),
                config.getReplyTo(),
                config.getSmtpHost(),
                config.getSmtpPort(),
                config.getSmtpSecurity(),
                config.getSmtpUsername(),
                config.isActivo(),
                config.isVerificado(),
                config.getEstado(),
                config.isAvisosHabilitados(),
                config.isReportesHabilitados(),
                config.isDobleFactorHabilitado(),
                config.getFechaVerificacion(),
                config.getUltimoError(),
                !isBlank(config.getSmtpPasswordEncrypted()),
                config.getUpdatedAt()
        );
    }

    private PlatformEmailConfigResponse updateVerification(Long configId, boolean verified, String error) {
        PlatformEmailConfig current = repository.findById(configId)
                .orElseThrow(() -> BusinessException.internal(
                        "PLATFORM_EMAIL_CONFIG_DISAPPEARED",
                        "La configuracion de correo de Azurion ya no existe."
                ));
        current.setVerificado(verified);
        current.setActivo(verified);
        current.setEstado(verified ? TenantEmailConfigStatus.VERIFICADO : TenantEmailConfigStatus.ERROR);
        current.setFechaVerificacion(verified ? LocalDateTime.now() : null);
        current.setUltimoError(verified ? null : trimToMax(error, 1000));
        return toResponse(repository.save(current));
    }

    private PlatformEmailConfig findOrThrow() {
        return repository.findByConfigKey(CONFIG_KEY)
                .orElseThrow(() -> new BusinessException(
                        "PLATFORM_EMAIL_CONFIG_NOT_FOUND",
                        "No existe una configuracion de correo global para Azurion."
                ));
    }

    private <T> T inTransaction(java.util.function.Supplier<T> action) {
        return new TransactionTemplate(transactionManager).execute(status -> action.get());
    }

    private boolean changed(String current, String next) {
        return !Objects.equals(trim(current), trim(next));
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String emptyToNull(String value) {
        String text = trim(value);
        return isBlank(text) ? null : text;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimToMax(String value, int max) {
        String text = trim(value);
        return text == null || text.length() <= max ? text : text.substring(0, max);
    }
}
