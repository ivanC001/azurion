package com.azurion.saascore.ventas.application.dto;

import java.math.BigDecimal;

public record VentaSummaryResponse(
        long totalVentas,
        BigDecimal totalMonto,
        long ventasHoy,
        long aceptadasSunat,
        long pendientesSunat,
        long ticketsInternos
) {
}
