package com.azurion.saascore.caja.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CerrarCajaRequest(
        @NotNull @DecimalMin(value = "0.00", inclusive = true) BigDecimal saldoSalida,
        @NotBlank String responsableId,
        @NotBlank String responsableNombre,
        String observacion
) {
}
