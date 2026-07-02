package com.azurion.saascore.crm.application.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record CrmActividadResponse(
        Long id,
        Long prospectoId,
        String prospectoNombre,
        Long oportunidadId,
        String oportunidadTitulo,
        Long clienteId,
        String clienteNombre,
        String tipoActividad,
        String asunto,
        String descripcion,
        OffsetDateTime fechaProgramada,
        OffsetDateTime fechaRealizada,
        String estado,
        String usuarioId,
        String resultado,
        String resultadoContacto,
        String nivelInteres,
        String estadoProspectoResultado,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
