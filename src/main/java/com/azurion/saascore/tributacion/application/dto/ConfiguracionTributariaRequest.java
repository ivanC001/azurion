package com.azurion.saascore.tributacion.application.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ConfiguracionTributariaRequest(
        @NotBlank String tipoOperacionDefaultId,
        @NotBlank String tipoAfectacionDefaultId,
        @NotBlank String tributoDefaultId,
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal porcentajeIgvDefault,
        @NotBlank String monedaDefault,
        String estado
) {
}
