package com.azurion.saascore.tributacion.application.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ProductoTributariaRequest(
        @NotNull Boolean usaConfiguracionEmpresa,
        Boolean afectoIgv,
        String tipoAfectacionIgvId,
        String tributoId,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal porcentajeImpuesto
) {
}
