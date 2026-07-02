package com.azurion.saascore.crm.application.dto;

public record CrmEtapaPipelineResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        Integer orden,
        Integer probabilidadDefault,
        String color,
        String icono,
        boolean ganado,
        boolean perdido,
        boolean requiereValidacion,
        String modoValidacion,
        boolean activo
) {
}
