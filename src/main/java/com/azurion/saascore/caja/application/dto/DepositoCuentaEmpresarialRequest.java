package com.azurion.saascore.caja.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record DepositoCuentaEmpresarialRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal monto,
        @NotBlank String cuentaEmpresarial,
        String numeroOperacion,
        @NotBlank String responsableId,
        @NotBlank String responsableNombre,
        String observacion
) {
}
