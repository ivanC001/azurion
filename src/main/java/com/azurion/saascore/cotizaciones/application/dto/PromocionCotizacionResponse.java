package com.azurion.saascore.cotizaciones.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PromocionCotizacionResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        String tipoDescuento,
        BigDecimal valor,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String estado
) {
}
