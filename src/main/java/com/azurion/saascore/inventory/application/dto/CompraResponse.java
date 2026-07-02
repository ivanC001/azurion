package com.azurion.saascore.inventory.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record CompraResponse(
        Long id,
        Long proveedorId,
        String proveedorDocumento,
        String proveedorNombre,
        String tipoComprobante,
        String serie,
        String correlativo,
        String numeroComprobante,
        LocalDate fechaEmision,
        OffsetDateTime fechaIngreso,
        Long almacenId,
        String almacenCodigo,
        String almacenNombre,
        BigDecimal total,
        BigDecimal ventaProyectada,
        BigDecimal gananciaProyectada,
        BigDecimal margenPorcentaje,
        String estado,
        List<CompraDetalleResponse> detalles
) {
}
