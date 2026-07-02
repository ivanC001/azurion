package com.azurion.saascore.cotizaciones.application.dto;

public record CotizacionPdfResponse(
        String fileName,
        String contentType,
        String base64
) {
}
