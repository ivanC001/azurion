package com.azurion.saascore.settings.email.application.dto;

import com.azurion.saascore.settings.email.domain.entities.SmtpSecurity;
import com.azurion.saascore.settings.email.domain.entities.TenantEmailConfigStatus;
import java.time.LocalDateTime;

public record PlatformEmailConfigResponse(
        Long id,
        String nombreRemitente,
        String correoRemitente,
        String replyTo,
        String smtpHost,
        Integer smtpPort,
        SmtpSecurity smtpSecurity,
        String smtpUsername,
        Boolean activo,
        Boolean verificado,
        TenantEmailConfigStatus estado,
        Boolean avisosHabilitados,
        Boolean reportesHabilitados,
        Boolean dobleFactorHabilitado,
        LocalDateTime fechaVerificacion,
        String ultimoError,
        Boolean smtpPasswordConfigured,
        LocalDateTime updatedAt
) {
}
