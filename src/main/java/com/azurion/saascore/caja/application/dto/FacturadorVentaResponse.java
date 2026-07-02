package com.azurion.saascore.caja.application.dto;

public record FacturadorVentaResponse(
        boolean success,
        int status,
        String endpoint,
        String tipoComprobante,
        String message,
        Object data
) {
}
