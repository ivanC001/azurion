package com.azurion.saascore.messaging.application.dto;

import com.azurion.saascore.messaging.domain.entities.MessageAudience;
import com.azurion.saascore.messaging.domain.entities.MessagePriority;
import java.time.LocalDateTime;

public record SentPlatformMessageResponse(
        Long id,
        String asunto,
        String contenido,
        MessagePriority prioridad,
        MessageAudience audiencia,
        String tenantId,
        String enviadoPor,
        LocalDateTime publicadoEn,
        LocalDateTime expiraEn,
        boolean activo,
        long recipientCount,
        long readCount
) {
}
