package com.azurion.saascore.inventory.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StockLoteResponse(
        Long id,
        Long loteId,
        String codigoLote,
        Long productoId,
        String productoSku,
        String productoNombre,
        Long almacenId,
        String almacenCodigo,
        String almacenNombre,
        BigDecimal stockActual,
        LocalDate fechaIngreso,
        LocalDate fechaVencimiento,
        String estado
) {
}
