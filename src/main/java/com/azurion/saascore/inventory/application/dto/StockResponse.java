package com.azurion.saascore.inventory.application.dto;

import java.math.BigDecimal;

public record StockResponse(
        Long id,
        Long productoId,
        String productoSku,
        String productoNombre,
        Long almacenId,
        String almacenCodigo,
        String almacenNombre,
        BigDecimal cantidad,
        BigDecimal stockMinimo,
        BigDecimal stockMaximo,
        String ubicacionFisica,
        boolean stockBajo,
        boolean sinStock
) {
}
