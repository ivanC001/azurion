package com.azurion.saascore.cotizaciones.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CotizacionDetalleRequest(
        Long productoId,
        Long promocionId,
        String descripcion,
        @Positive BigDecimal cantidad,
        @DecimalMin("0.00") BigDecimal precioUnitario,
        @DecimalMin("0.00") BigDecimal descuento
) {
}
