package com.azurion.saascore.crm.application.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record WhatsappConnectionStatusResponse(
        boolean activo,
        boolean configuracionCompleta,
        boolean accesoMetaValido,
        boolean wabaSuscrita,
        boolean webhookVerificado,
        boolean conectado,
        String displayPhoneNumber,
        String verifiedName,
        String qualityRating,
        OffsetDateTime tokenExpiresAt,
        List<String> permissions,
        String message,
        OffsetDateTime testedAt,
        OffsetDateTime webhookVerifiedAt
) {
}
