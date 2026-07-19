package com.azurion.saascore.crm.application.dto;

import java.time.OffsetDateTime;

public record CrmCanalTokenConfigResponse(
        Long id,
        String canal,
        String nombre,
        String accessToken,
        String verifyToken,
        String webhookUrl,
        String appId,
        String phoneNumberId,
        String wabaId,
        Boolean accessTokenConfigured,
        Boolean verifyTokenConfigured,
        Boolean appSecretConfigured,
        OffsetDateTime webhookVerifiedAt,
        OffsetDateTime lastConnectionTestAt,
        Boolean lastConnectionOk,
        String lastConnectionMessage,
        Boolean wabaSubscribed,
        String metaDisplayPhoneNumber,
        String metaVerifiedName,
        String metaQualityRating,
        OffsetDateTime metaTokenExpiresAt,
        Boolean activo,
        String metadataJson
) {
}
