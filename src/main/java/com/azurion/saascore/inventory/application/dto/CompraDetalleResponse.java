package com.azurion.saascore.inventory.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CompraDetalleResponse(
        Long id,
        Long productoId,
        String productoSku,
        String productoNombre,
        BigDecimal cantidad,
        BigDecimal costoUnitario,
        BigDecimal precioVenta,
        BigDecimal total,
        BigDecimal ventaProyectada,
        BigDecimal gananciaProyectada,
        BigDecimal margenPorcentaje,
        String codigoLote,
        LocalDate fechaFabricacion,
        LocalDate fechaVencimiento
) {
}
