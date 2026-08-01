package com.azurion.saascore.caja.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CerrarCajaTurnoRequest(
        @NotNull @DecimalMin(value = "0.00", inclusive = true) BigDecimal conteoFisico,
        @Size(max = 500) String observacion
) {
}
