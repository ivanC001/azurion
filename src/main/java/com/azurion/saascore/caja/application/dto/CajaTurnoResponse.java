package com.azurion.saascore.caja.application.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CajaTurnoResponse(
        Long id,
        String numero,
        Long cajaId,
        Long sucursalId,
        String sucursalCodigo,
        String sucursalNombre,
        String cajaCodigo,
        String cajaNombre,
        String moneda,
        String estado,
        Long usuarioId,
        BigDecimal saldoApertura,
        BigDecimal saldoEsperado,
        BigDecimal conteoFisico,
        BigDecimal diferenciaCierre,
        Integer numeroVentas,
        BigDecimal totalVentas,
        BigDecimal totalEfectivo,
        BigDecimal totalTarjeta,
        BigDecimal totalBilleteraDigital,
        BigDecimal totalTransferencia,
        BigDecimal totalCredito,
        BigDecimal totalIngresosManuales,
        BigDecimal totalRetiros,
        BigDecimal totalDepositos,
        BigDecimal totalReembolsos,
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
