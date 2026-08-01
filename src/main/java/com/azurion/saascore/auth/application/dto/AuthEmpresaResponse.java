package com.azurion.saascore.auth.application.dto;

public record AuthEmpresaResponse(
        Long id,
        String ruc,
        String razonSocial,
        String tenantId,
        String schemaName,
        String logoPanelUrl,
        String tipoDocumentoFiscal,
        String nombreComercial,
        String paisCodigo,
        String paisNombre,
        String zonaHoraria,
        String idioma,
        String formatoFecha,
        String formatoHora,
        String monedaCodigo,
        String monedaSimbolo,
        String facturadorStatus,
        String facturadorDocumentMode,
        String facturadorFiscalStatus,
        String facturadorSunatMode,
        boolean activo
) {
}
