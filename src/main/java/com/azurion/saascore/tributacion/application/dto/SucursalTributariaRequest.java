package com.azurion.saascore.tributacion.application.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public record SucursalTributariaRequest(
        String tipoOperacionDefaultId,
        String tipoAfectacionDefaultId,
        String tributoDefaultId,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal porcentajeIgvDefault
) {
}
