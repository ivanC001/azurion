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
        BigDecimal costoNetoUnitario,
        BigDecimal porcentajeIgv,
        BigDecimal montoIgvUnitario,
        BigDecimal costoTotalUnitario,
        BigDecimal costoInventariableUnitario,
        BigDecimal precioVenta,
        BigDecimal precioVentaNeto,
        BigDecimal subtotalNeto,
        BigDecimal montoIgv,
        BigDecimal total,
        BigDecimal totalCostoInventariable,
        BigDecimal ventaProyectada,
        BigDecimal gananciaProyectada,
        BigDecimal margenPorcentaje,
        String codigoLote,
        LocalDate fechaFabricacion,
        LocalDate fechaVencimiento
) {
}
