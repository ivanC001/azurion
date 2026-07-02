package com.azurion.saascore.caja.application.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CajaMovimientoResponse(
        Long id,
        Long cajaId,
        String tipoMovimiento,
        BigDecimal monto,
        BigDecimal saldoAnterior,
        BigDecimal saldoResultante,
        String descripcion,
        String referencia,
        String cuentaEmpresarial,
        String responsableId,
        String responsableNombre,
        OffsetDateTime fechaMovimiento
) {
}
