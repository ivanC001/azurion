package com.azurion.saascore.crm.application.dto;

import jakarta.validation.constraints.Size;

public record UpdateCrmCanalTokenConfigRequest(
        @Size(max = 40) String canal,
        @Size(max = 120) String nombre,
        @Size(max = 2000) String accessToken,
        @Size(max = 300) String verifyToken,
        @Size(max = 500) String webhookUrl,
        @Size(max = 180) String appId,
        @Size(max = 180) String phoneNumberId,
        Boolean activo,
        String metadataJson
) {
}
