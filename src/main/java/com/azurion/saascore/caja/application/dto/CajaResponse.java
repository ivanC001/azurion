package com.azurion.saascore.caja.application.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CajaResponse(
        Long id,
        Long sucursalId,
        String sucursalCodigo,
        String sucursalNombre,
        String codigo,
        String nombre,
        String estado,
        BigDecimal saldoCapital,
        BigDecimal saldoActual,
        BigDecimal saldoSalida,
        BigDecimal totalEntradas,
        BigDecimal totalSalidas,
        BigDecimal totalDepositos,
        BigDecimal diferenciaCierre,
        String responsableAperturaId,
        String responsableAperturaNombre,
        String responsableCierreId,
        String responsableCierreNombre,
        OffsetDateTime fechaApertura,
        OffsetDateTime fechaCierre,
        String observacionApertura,
        String observacionCierre
) {
}
