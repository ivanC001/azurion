package com.azurion.saascore.inventory.application.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CompraDetalleRequest(
        @NotNull Long productoId,
        @NotNull BigDecimal cantidad,
        @NotNull BigDecimal costoUnitario,
        BigDecimal precioVenta,
        String codigoLote,
        LocalDate fechaFabricacion,
        LocalDate fechaVencimiento
) {
}
