package com.azurion.saascore.crm.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateCrmCurrencyConfigRequest(
        @NotBlank @Size(min = 3, max = 3) String moneda,
        @Size(max = 80) String nombre,
        @Size(max = 8) String simbolo,
        @DecimalMin(value = "0.000001") BigDecimal tipoCambioBase,
        @DecimalMin(value = "0.00") BigDecimal margenConversionPorcentaje,
        Boolean activo
) {
}
