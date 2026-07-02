package com.azurion.saascore.planes.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record UpdatePlanRequest(
        @NotBlank String nombre,
        String descripcion,
        @NotNull @Min(value = 0) Long limiteMensualBolsa,
        @NotNull @DecimalMin(value = "0.00", inclusive = true) BigDecimal precioMensual,
        @NotBlank String estado,
        List<String> moduloCodigos
) {
}
