package com.azurion.saascore.reportes.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FiscalSummaryResponse(
        LocalDate desde,
        LocalDate hasta,
        BigDecimal ventasBrutas,
        BigDecimal ventasNetas,
        BigDecimal debitoFiscal,
        BigDecimal comprasNetas,
        BigDecimal igvCompras,
        BigDecimal creditoFiscal,
        BigDecimal igvPorPagarEstimado,
        BigDecimal saldoCreditoFiscalEstimado,
        BigDecimal costoVentasConocido,
        BigDecimal margenReal,
        boolean margenCompleto,
        long lineasVentaSinCostoHistorico,
        long notasHistoricasSinDesglose,
        long notasCreditoSinReversionCosto
) {
}
