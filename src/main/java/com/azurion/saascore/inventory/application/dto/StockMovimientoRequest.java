package com.azurion.saascore.inventory.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record StockMovimientoRequest(
        @NotNull Long productoId,
        @NotNull Long almacenId,
        Long almacenDestinoId,
        Long loteId,
        String codigoLote,
        LocalDate fechaFabricacion,
        LocalDate fechaVencimiento,
        String tipoMovimiento,
        @NotBlank String motivo,
        @NotNull BigDecimal cantidad,
        BigDecimal costoUnitario,
        BigDecimal precioCompra,
        BigDecimal precioVenta,
        String usuarioId,
        String referencia
) {
}
