package com.azurion.saascore.auth.application.dto;

public record AuthEmpresaResponse(
        Long id,
        String ruc,
        String razonSocial,
        String tenantId,
        String schemaName,
        String logoPanelUrl,
        boolean activo
) {
}
