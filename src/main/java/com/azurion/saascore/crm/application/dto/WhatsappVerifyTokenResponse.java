package com.azurion.saascore.crm.application.dto;

import java.time.OffsetDateTime;

public record WhatsappVerifyTokenResponse(
        String verifyToken,
        OffsetDateTime generadoEn
) {
}
