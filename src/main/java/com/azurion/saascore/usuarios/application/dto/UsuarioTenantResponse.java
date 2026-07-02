package com.azurion.saascore.usuarios.application.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UsuarioTenantResponse(
        Long id,
        String username,
        String nombres,
        String email,
        boolean activo,
        List<String> roles,
        List<UsuarioSucursalResponse> sucursales,
        LocalDateTime ultimoAcceso
) {
}
