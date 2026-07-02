package com.azurion.saascore.cotizaciones.application.dto;

import java.math.BigDecimal;

public record CotizacionDetalleResponse(
        Long id,
        Long productoId,
        String productoSku,
        String productoNombre,
        Long promocionId,
        String promocionNombre,
        String descripcion,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal descuento,
        BigDecimal promocionDescuento,
        BigDecimal total
) {
}
