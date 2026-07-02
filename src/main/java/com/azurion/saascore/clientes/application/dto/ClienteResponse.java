package com.azurion.saascore.clientes.application.dto;

import java.math.BigDecimal;

public record ClienteResponse(
        Long id,
        String tipoDocumento,
        String numeroDocumento,
        String nombre,
        String email,
        String direccion,
        String ubigeo,
        String telefono,
        BigDecimal limiteCredito,
        BigDecimal saldoDeuda,
        BigDecimal creditoDisponible,
        Integer diasCredito,
        Boolean deudor,
        Boolean activo
) {
}
