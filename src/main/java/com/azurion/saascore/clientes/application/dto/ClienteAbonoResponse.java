package com.azurion.saascore.clientes.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ClienteAbonoResponse(
        Long id,
        Long clienteId,
        BigDecimal monto,
        BigDecimal saldoAnterior,
        BigDecimal saldoResultante,
        String observacion,
        LocalDateTime fecha
) {
}
