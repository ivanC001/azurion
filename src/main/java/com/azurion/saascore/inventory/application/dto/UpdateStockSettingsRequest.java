package com.azurion.saascore.inventory.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateStockSettingsRequest(
        @NotNull @DecimalMin("0.00") BigDecimal stockMinimo,
        @DecimalMin("0.00") BigDecimal stockMaximo,
        @Size(max = 120) String ubicacionFisica
) {
}
