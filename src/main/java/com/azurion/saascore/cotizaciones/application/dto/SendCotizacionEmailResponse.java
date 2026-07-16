package com.azurion.saascore.cotizaciones.application.dto;

public record SendCotizacionEmailResponse(
        CotizacionResponse cotizacion,
        String destinatario
) {
}
