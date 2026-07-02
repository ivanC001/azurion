package com.azurion.saascore.usuarios.application.dto;

public record UsuarioTenantRolResponse(
        Long id,
        Long usuarioGlobalId,
        String tenantId,
        String rolCodigo,
        boolean activo,
        Long asignadoPorUsuarioId
) {
}
