package com.azurion.saascore.inventory.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoteOrigenResponse(
        Long loteId,
        String codigoLote,
        Long productoId,
        String productoSku,
        String productoNombre,
        LocalDate fechaIngreso,
        LocalDate fechaVencimiento,
        BigDecimal cantidadInicial,
        BigDecimal costoUnitario,
        Long compraId,
        String tipoComprobante,
        String numeroComprobante,
        LocalDate fechaEmision,
        Long proveedorId,
        String proveedorDocumento,
        String proveedorNombre
) {
}
