package com.azurion.saascore.planes.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.List;

public record CreatePlanRequest(
        @NotBlank String nombre,
        @NotBlank
        @Pattern(regexp = "^[A-Z0-9_]{3,40}$", message = "codigo must match ^[A-Z0-9_]{3,40}$")
        String codigo,
        String descripcion,
        @NotNull @Min(value = 0) Long limiteMensualBolsa,
        @NotNull @Min(value = 1) Integer limiteUsuarios,
        @NotNull @DecimalMin(value = "0.00", inclusive = true) BigDecimal precioMensual,
        List<String> moduloCodigos
) {
}
