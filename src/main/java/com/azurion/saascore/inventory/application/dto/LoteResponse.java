package com.azurion.saascore.inventory.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoteResponse(
        Long id,
        Long productoId,
        String productoSku,
        String productoNombre,
        String codigoLote,
        LocalDate fechaIngreso,
        LocalDate fechaVencimiento,
        BigDecimal cantidadInicial,
        BigDecimal costoUnitario,
        String estado,
        Long compraDetalleId
) {
}
