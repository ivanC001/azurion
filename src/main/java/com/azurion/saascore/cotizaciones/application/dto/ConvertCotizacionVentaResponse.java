package com.azurion.saascore.cotizaciones.application.dto;

import com.azurion.saascore.caja.application.dto.RegistrarVentaCajaResponse;

public record ConvertCotizacionVentaResponse(
        CotizacionResponse cotizacion,
        RegistrarVentaCajaResponse venta
) {
}
