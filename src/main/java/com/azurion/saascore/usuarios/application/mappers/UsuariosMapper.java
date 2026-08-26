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
                .sorted()
                .toList();

        return new UsuarioTenantResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getCargo(),
                usuario.getFotoPerfilUrl(),
                usuario.isActivo(),
                roles,
                sucursales,
                usuario.getUltimoAcceso()
        );
    }
}
