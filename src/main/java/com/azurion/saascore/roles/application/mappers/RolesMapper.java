package com.azurion.saascore.roles.application.mappers;

import com.azurion.saascore.roles.application.dto.PermisoResponse;
import com.azurion.saascore.roles.application.dto.RolResponse;
import com.azurion.saascore.roles.application.support.RolesBusinessRules;
import com.azurion.saascore.roles.domain.entities.Permiso;
import com.azurion.saascore.roles.domain.entities.Rol;
import java.util.Comparator;
import java.util.List;

public final class RolesMapper {

    private RolesMapper() {
    }

    public static RolResponse toRolResponse(Rol rol) {
        List<PermisoResponse> permisos = rol.getRolPermisos().stream()
                .map(rp -> toPermisoResponse(rp.getPermiso()))
                .sorted(Comparator.comparing(PermisoResponse::modulo).thenComparing(PermisoResponse::nombre))
                .toList();

        return new RolResponse(
                rol.getId(),
                rol.getCodigo(),
                rol.getNombre(),
                rol.getDescripcion(),
                rol.isActivo(),
                rol.isSistema(),
                RolesBusinessRules.canEditRole(rol),
                RolesBusinessRules.canDeleteRole(rol),
                RolesBusinessRules.canManagePermissions(rol),
                permisos
        );
    }

    public static PermisoResponse toPermisoResponse(Permiso permiso) {
        return new PermisoResponse(
                permiso.getId(),
                permiso.getCodigo(),
                permiso.getNombre(),
                permiso.getDescripcion(),
                permiso.getModulo(),
                permiso.isActivo(),
                permiso.isSistema(),
                RolesBusinessRules.canEditPermiso(permiso),
                RolesBusinessRules.canDeletePermiso(permiso)
        );
    }
}
