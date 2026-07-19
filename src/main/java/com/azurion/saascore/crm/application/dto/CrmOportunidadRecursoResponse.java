package com.azurion.saascore.crm.application.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record CrmOportunidadRecursoResponse(
        Long id,
        Long oportunidadId,
        String tipo,
        Map<String, Object> data,
        boolean hasArchivo,
        String archivoNombre,
        String archivoMimeType,
        Long archivoSize,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
