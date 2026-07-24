package com.azurion.saascore.settings.email.application.dto;

import com.azurion.saascore.settings.email.domain.entities.SmtpSecurity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlatformEmailConfigRequest(
        @NotBlank @Size(max = 160) String nombreRemitente,
        @NotBlank @Email @Size(max = 180) String correoRemitente,
        @Email @Size(max = 180) String replyTo,
        @NotBlank @Size(max = 180) String smtpHost,
        @NotNull @Min(1) @Max(65535) Integer smtpPort,
        @NotNull SmtpSecurity smtpSecurity,
        @NotBlank @Size(max = 180) String smtpUsername,
        @Size(max = 500) String smtpPassword,
        Boolean activo,
        Boolean avisosHabilitados,
        Boolean reportesHabilitados,
        Boolean dobleFactorHabilitado
) {
}
