package com.azurion.saascore.empresas.application.dto;

public record EmpresaResponse(
        Long id,
        String ruc,
        String razonSocial,
        String tenantId,
        String schemaName,
        String logoPanelUrl,
        boolean activo
) {
}
