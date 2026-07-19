package com.azurion.saascore.crm.application.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record CrmWhatsappMessageResponse(
        Long id,
        Long prospectoId,
        String metaMessageId,
        String direccion,
        String remitente,
        String destinatario,
        String tipoMensaje,
        String contenido,
        String estado,
        OffsetDateTime mensajeEn,
        OffsetDateTime leidoEn,
        LocalDateTime createdAt
) {
}
