package com.azurion.saascore.facturacion.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record RegistrarNotaFiscalRequest(
        @NotNull Long ventaId,
        @NotBlank String motivoCodigo,
        @NotBlank String motivoDescripcion,
        @NotNull @Positive BigDecimal monto,
        @NotBlank String responsableId,
        @NotBlank String responsableNombre,
        @NotBlank @Size(max = 100) String clientOperationId
) {
}
