package com.azurion.saascore.crm.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record CrmOportunidadResponse(
        Long id,
        Long prospectoId,
        String prospectoNombre,
        Long clienteId,
        String clienteNombre,
        String tipoOportunidad,
        Long catalogoItemId,
        String titulo,
        String descripcion,
        BigDecimal montoEstimado,
        BigDecimal montoReal,
        Integer probabilidad,
        Long etapaId,
        String etapa,
        String etapaNombre,
        String etapaColor,
        LocalDate fechaCierreEstimada,
        String responsableId,
        String estado,
        String motivoPerdida,
        OffsetDateTime fechaCierreReal,
        OffsetDateTime fechaUltimaActualizacion,
        OffsetDateTime fechaGanada,
        OffsetDateTime fechaPerdida,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
