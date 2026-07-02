package com.azurion.saascore.caja.application.dto;

import com.azurion.saascore.ventas.application.dto.VentaResponse;

public record RegistrarVentaCajaResponse(
        VentaResponse venta,
        CajaMovimientoResponse movimientoCaja,
        FacturadorVentaResponse facturacion
) {
}
