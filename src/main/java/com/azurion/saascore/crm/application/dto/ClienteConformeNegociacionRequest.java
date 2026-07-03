package com.azurion.saascore.crm.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ClienteConformeNegociacionRequest(
        Long cotizacionId,
        @DecimalMin("0.00") BigDecimal precioFinal,
        @Size(max = 80) String formaPago,
        @Min(1) Integer cuotas,
        @Size(max = 2000) String observacion
) {
}
