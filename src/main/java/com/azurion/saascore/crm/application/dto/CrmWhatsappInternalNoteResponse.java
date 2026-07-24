package com.azurion.saascore.crm.application.dto;

import java.time.LocalDateTime;

public record CrmWhatsappInternalNoteResponse(
        Long id,
        Integer slot,
        String contenido,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
