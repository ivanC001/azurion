package com.azurion.saascore.auth.application.dto;

import com.azurion.saascore.usuarios.application.dto.UsuarioSucursalResponse;
import java.util.List;

public record CurrentUserProfileResponse(
        Long id,
        String username,
        String nombres,
        String apellidos,
        String email,
        String telefono,
        String cargo,
        String fotoPerfilUrl,
        boolean activo,
        boolean puedeEditarDatosPersonales,
        boolean puedeCambiarContrasena,
        String tipoCuenta,
        List<String> roles,
        List<UsuarioSucursalResponse> sucursales
) {
}
