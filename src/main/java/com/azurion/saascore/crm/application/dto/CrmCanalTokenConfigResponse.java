package com.azurion.saascore.crm.application.dto;

public record CrmCanalTokenConfigResponse(
        Long id,
        String canal,
        String nombre,
        String accessToken,
        String verifyToken,
        String webhookUrl,
        String appId,
        String phoneNumberId,
        Boolean activo,
        String metadataJson
) {
}
