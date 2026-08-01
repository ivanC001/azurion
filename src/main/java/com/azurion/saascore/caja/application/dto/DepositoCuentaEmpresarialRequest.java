package com.azurion.saascore.caja.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record DepositoCuentaEmpresarialRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal monto,
        @NotBlank String cuentaEmpresarial,
        String numeroOperacion,
        String observacion,
        @NotBlank @Size(max = 100) String clientOperationId
) {
}
