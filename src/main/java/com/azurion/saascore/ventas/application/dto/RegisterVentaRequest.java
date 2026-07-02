package com.azurion.saascore.ventas.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record RegisterVentaRequest(
        @NotBlank String externalId,
        @NotBlank String clienteDocumento,
        @NotBlank String clienteNombre,
        @NotBlank String moneda,
        @NotNull BigDecimal total,
        @NotEmpty List<@Valid VentaItemRequest> items
) {

    public record VentaItemRequest(
            Long productoId,
            @NotBlank String sku,
            @NotBlank String description,
            @NotNull BigDecimal quantity,
            @NotNull BigDecimal unitPrice,
            @NotNull BigDecimal discount,
            @NotBlank String tipoOperacionCodigo,
            @NotBlank String tipoAfectacionIgvCodigo,
            @NotBlank String tributoCodigo,
            @NotNull BigDecimal porcentajeIgv,
            @NotNull BigDecimal baseImponible,
            @NotNull BigDecimal montoIgv,
            @NotNull BigDecimal lineTotal
    ) {
    }
}
