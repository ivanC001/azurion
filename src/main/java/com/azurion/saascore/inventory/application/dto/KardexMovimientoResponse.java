package com.azurion.saascore.inventory.application.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record KardexMovimientoResponse(
        Long id,
        Long productoId,
        String productoSku,
        String productoNombre,
        Long almacenId,
        String almacenCodigo,
        String tipoMovimiento,
        String motivo,
        BigDecimal cantidad,
        BigDecimal saldoResultante,
        String referencia,
        OffsetDateTime fechaMovimiento
) {
}
