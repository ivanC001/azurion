package com.azurion.saascore.crm.application.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CrmLeadNotificationDispatchResponse(
        Long id,
        Long prospectoId,
        String prospectoNombre,
        String tipo,
        List<String> destinatarios,
        String asunto,
        String estado,
        String detalle,
        LocalDateTime createdAt,
        LocalDateTime processedAt
) {
}
