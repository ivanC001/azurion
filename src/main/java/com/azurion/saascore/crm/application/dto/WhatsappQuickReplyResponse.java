package com.azurion.saascore.crm.application.dto;

import java.time.LocalDateTime;

public record WhatsappQuickReplyResponse(
        Long id,
        Integer slot,
        String titulo,
        String mensaje,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
