package com.azurion.saascore.roles.application.dto;

public record PermisoResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        String modulo,
        boolean activo,
        boolean sistema,
        boolean editable,
        boolean eliminable
) {
}
