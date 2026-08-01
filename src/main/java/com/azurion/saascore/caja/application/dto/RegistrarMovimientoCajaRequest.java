package com.azurion.saascore.caja.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record RegistrarMovimientoCajaRequest(
        @NotBlank String tipoMovimiento,
        @NotNull @DecimalMin(value = "0.01") BigDecimal monto,
        @NotBlank @Size(max = 250) String descripcion,
        @Size(max = 120) String referencia,
        @NotBlank @Size(max = 100) String clientOperationId
) {
}
