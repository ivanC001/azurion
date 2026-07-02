package com.azurion.saascore.cotizaciones.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePromocionCotizacionRequest(
        @NotBlank String codigo,
        @NotBlank String nombre,
        String descripcion,
        @NotBlank String tipoDescuento,
        @NotNull @DecimalMin("0.00") BigDecimal valor,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String estado
) {
}
