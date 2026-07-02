package com.azurion.saascore.usuarios.application.mappers;

import com.azurion.saascore.usuarios.application.dto.UsuarioTenantResponse;
import com.azurion.saascore.usuarios.domain.entities.UsuarioTenant;
import java.util.Comparator;
import java.util.List;

public final class UsuariosMapper {

    private UsuariosMapper() {
    }

    public static UsuarioTenantResponse toResponse(UsuarioTenant usuario, List<com.azurion.saascore.usuarios.application.dto.UsuarioSucursalResponse> sucursales) {
        List<String> roles = usuario.getUsuarioRoles().stream()
                .map(usuarioRol -> usuarioRol.getRol().getCodigo())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();

        return new UsuarioTenantResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getNombres(),
                usuario.getEmail(),
                usuario.isActivo(),
                roles,
                sucursales,
                usuario.getUltimoAcceso()
        );
    }
}
