package com.azurion.saascore.roles.application.dto;

import com.azurion.saascore.roles.domain.entities.RoleScope;
import java.util.List;

public record RolResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        RoleScope ambito,
        boolean activo,
        boolean sistema,
        boolean deprecated,
        boolean editable,
        boolean eliminable,
        boolean gestionaPermisos,
        List<PermisoResponse> permisos
) {
}
