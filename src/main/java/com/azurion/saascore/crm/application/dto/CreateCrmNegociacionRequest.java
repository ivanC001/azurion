package com.azurion.saascore.crm.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateCrmNegociacionRequest(
        Long cotizacionId,
        @Size(max = 40) String estado,
        @Size(max = 80) String solicitudCliente,
        @DecimalMin("0.00") BigDecimal precioOriginal,
        @DecimalMin("0.00") BigDecimal descuento,
        @DecimalMin("0.00") BigDecimal precioFinal,
        @Size(max = 80) String formaPago,
        @Min(1) Integer cuotas,
        LocalDate fechaInicio,
        LocalDate fechaEntrega,
        @Size(max = 2000) String observacion,
        @Size(max = 40) String resultado
) {
}
