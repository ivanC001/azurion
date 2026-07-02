package com.azurion.saascore.auth.application.dto;

import java.time.OffsetDateTime;
import java.util.List;
import com.azurion.saascore.usuarios.application.dto.UsuarioSucursalResponse;

public record TenantLoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String username,
        Long userId,
        String nombres,
        String email,
        String tenantId,
        AuthEmpresaResponse empresa,
        List<String> roles,
        List<String> permissions,
        List<String> modules,
        List<UsuarioSucursalResponse> sucursales,
        boolean adminEmpresa,
        OffsetDateTime issuedAt
) {
}
