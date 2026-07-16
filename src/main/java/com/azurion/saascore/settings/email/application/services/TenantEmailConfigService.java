package com.azurion.saascore.settings.email.application.services;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.settings.email.application.dto.EmailConfigRequest;
import com.azurion.saascore.settings.email.application.dto.EmailConfigResponse;
import com.azurion.saascore.settings.email.application.dto.TestEmailRequest;
import com.azurion.saascore.settings.email.domain.entities.TenantEmailConfig;
import com.azurion.saascore.settings.email.domain.entities.TenantEmailConfigStatus;
import com.azurion.saascore.settings.email.domain.repositories.TenantEmailConfigRepository;
import com.azurion.shared.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantEmailConfigService {

    private final TenantEmailConfigRepository repository;
    private final EmailSecretEncryptionService encryptionService;
    private final SmtpEmailTransportService smtpTransport;

    @Transactional(readOnly = true)
    public EmailConfigResponse getCurrentTenantConfig() {
        return repository.findByTenantId(currentTenant())
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional
    public EmailConfigResponse saveOrUpdateConfig(EmailConfigRequest request) {
        String tenantId = currentTenant();
        TenantEmailConfig config = repository.findByTenantId(tenantId).orElseGet(() -> {
            TenantEmailConfig item = new TenantEmailConfig();
            item.setTenantId(tenantId);
            item.setEstado(TenantEmailConfigStatus.PENDIENTE);
            return item;
        });
        boolean creating = config.getId() == null;
        if (creating && isBlank(request.smtpPassword())) {
            throw new BusinessException("EMAIL_SMTP_PASSWORD_REQUIRED", "La contrasena SMTP es obligatoria al crear la configuracion.");
        }

        String newEncryptedPassword = null;
        if (!isBlank(request.smtpPassword())) {
            newEncryptedPassword = encryptionService.encrypt(request.smtpPassword().trim());
        }
        boolean smtpChanged = creating
                || changed(config.getSmtpHost(), request.smtpHost())
                || !Objects.equals(config.getSmtpPort(), request.smtpPort())
                || config.getSmtpSecurity() != request.smtpSecurity()
                || changed(config.getSmtpUsername(), request.smtpUsername())
                || newEncryptedPassword != null;

        config.setNombreRemitente(trim(request.nombreRemitente()));
        config.setCorreoRemitente(trim(request.correoRemitente()));
        config.setReplyTo(trim(request.replyTo()));
        config.setSmtpHost(trim(request.smtpHost()));
        config.setSmtpPort(request.smtpPort());
        config.setSmtpSecurity(request.smtpSecurity());
        config.setSmtpUsername(trim(request.smtpUsername()));
        if (newEncryptedPassword != null) {
            config.setSmtpPasswordEncrypted(newEncryptedPassword);
        }
        config.setActivo(Boolean.TRUE.equals(request.activo()));
        if (!config.isActivo()) {
            config.setVerificado(false);
            config.setEstado(TenantEmailConfigStatus.INACTIVO);
        } else if (smtpChanged) {
            config.setVerificado(false);
            config.setEstado(TenantEmailConfigStatus.PENDIENTE);
            config.setFechaVerificacion(null);
            config.setUltimoError(null);
        }
        return toResponse(repository.save(config));
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public EmailConfigResponse testEmailConfig(TestEmailRequest request) {
        TenantEmailConfig config = repository.findByTenantId(currentTenant())
                .orElseThrow(() -> new BusinessException("EMAIL_CONFIG_NOT_FOUND", "Configura el correo SMTP antes de enviar la prueba."));
        try {
            smtpTransport.send(
                    config,
                    trim(request.correoDestino()),
                    "Correo de prueba Azurion CRM",
                    "Tu configuracion de correo SMTP quedo verificada para enviar mensajes desde Azurion CRM.",
                    List.of()
            );
            config.setVerificado(true);
            config.setActivo(true);
            config.setEstado(TenantEmailConfigStatus.VERIFICADO);
            config.setFechaVerificacion(LocalDateTime.now());
            config.setUltimoError(null);
        } catch (BusinessException ex) {
            config.setVerificado(false);
            config.setEstado(TenantEmailConfigStatus.ERROR);
            config.setUltimoError(trimToMax(ex.getMessage(), 1000));
            repository.save(config);
            throw ex;
        }
        return toResponse(repository.save(config));
    }

    @Transactional
    public EmailConfigResponse activate() {
        TenantEmailConfig config = repository.findByTenantId(currentTenant())
                .orElseThrow(() -> new BusinessException("EMAIL_CONFIG_NOT_FOUND", "No existe configuracion de correo."));
        if (!config.isVerificado()) {
            throw new BusinessException("EMAIL_CONFIG_NOT_VERIFIED", "Verifica el correo antes de activarlo.");
        }
        config.setActivo(true);
        config.setEstado(TenantEmailConfigStatus.VERIFICADO);
        return toResponse(repository.save(config));
    }

    @Transactional
    public EmailConfigResponse deactivate() {
        TenantEmailConfig config = repository.findByTenantId(currentTenant())
                .orElseThrow(() -> new BusinessException("EMAIL_CONFIG_NOT_FOUND", "No existe configuracion de correo."));
        config.setActivo(false);
        config.setEstado(TenantEmailConfigStatus.INACTIVO);
        return toResponse(repository.save(config));
    }

    @Transactional(readOnly = true)
    public TenantEmailConfig getVerifiedConfigOrThrow(String tenantId) {
        if (!currentTenant().equals(tenantId)) {
            throw new BusinessException("EMAIL_TENANT_SCOPE_INVALID", "No puedes usar la configuracion de otro tenant.");
        }
        TenantEmailConfig config = repository.findByTenantId(tenantId)
                .orElseThrow(() -> new BusinessException(
                        "EMAIL_CONFIG_NOT_FOUND",
                        "No hay un correo configurado para esta empresa. Configuralo en Configuracion CRM > Correo."
                ));
        if (!config.isActivo() || !config.isVerificado() || config.getEstado() != TenantEmailConfigStatus.VERIFICADO) {
            throw new BusinessException(
                    "EMAIL_CONFIG_NOT_VERIFIED",
                    "El correo de la empresa no esta activo y verificado. Revisalo en Configuracion CRM > Correo."
            );
        }
        return config;
    }

    public EmailConfigResponse toResponse(TenantEmailConfig config) {
        return new EmailConfigResponse(
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
                config.getFechaVerificacion(),
                config.getUltimoError(),
                !isBlank(config.getSmtpPasswordEncrypted())
        );
    }

    private String currentTenant() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank() || TenantContext.DEFAULT_TENANT.equalsIgnoreCase(tenantId)) {
            throw new BusinessException("TENANT_CONTEXT_REQUIRED", "Selecciona una empresa tenant para configurar correo.");
        }
        return tenantId;
    }

    private boolean changed(String current, String next) {
        return !Objects.equals(trim(current), trim(next));
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimToMax(String value, int max) {
        String text = trim(value);
        return text == null || text.length() <= max ? text : text.substring(0, max);
    }
}
