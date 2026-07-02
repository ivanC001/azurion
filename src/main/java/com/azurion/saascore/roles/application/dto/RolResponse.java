package com.azurion.saascore.roles.application.dto;

import java.util.List;

public record RolResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        boolean activo,
        boolean sistema,
        boolean editable,
        boolean eliminable,
        boolean gestionaPermisos,
        List<PermisoResponse> permisos
) {
}
