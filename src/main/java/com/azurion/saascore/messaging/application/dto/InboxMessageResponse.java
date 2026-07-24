package com.azurion.saascore.messaging.application.dto;

import com.azurion.saascore.messaging.domain.entities.MessageAudience;
import com.azurion.saascore.messaging.domain.entities.MessagePriority;
import java.time.LocalDateTime;

public record InboxMessageResponse(
        Long recipientId,
        Long messageId,
        String asunto,
        String contenido,
        MessagePriority prioridad,
        MessageAudience audiencia,
        String tenantId,
        String enviadoPor,
        LocalDateTime publicadoEn,
        LocalDateTime expiraEn,
        boolean leido,
        LocalDateTime leidoEn
) {
}
