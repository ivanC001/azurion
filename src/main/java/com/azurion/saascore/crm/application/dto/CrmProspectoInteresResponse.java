package com.azurion.saascore.crm.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record CrmProspectoInteresResponse(
        Long id,
        Long prospectoId,
        String landingKey,
        String campania,
        String canalIngreso,
        Long catalogoItemId,
        boolean productoPendiente,
        String tipoInteres,
        String interesPrincipal,
        String interesDetalle,
        String mensaje,
        BigDecimal presupuestoEstimado,
        LocalDate fechaInteres,
        String landingUrl,
        String metadataJson,
        Integer contadorEnvios,
        OffsetDateTime ultimoEnvioEn,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
