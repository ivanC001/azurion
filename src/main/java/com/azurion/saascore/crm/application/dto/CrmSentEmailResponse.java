package com.azurion.saascore.crm.application.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CrmSentEmailResponse(
        Long cotizacionId,
        Long oportunidadId,
        String destinatarioNombre,
        String destinatarioCorreo,
        String asunto,
        String moneda,
        BigDecimal total,
        String estado,
        String enviadoPor,
        OffsetDateTime enviadoEn
) {
}
