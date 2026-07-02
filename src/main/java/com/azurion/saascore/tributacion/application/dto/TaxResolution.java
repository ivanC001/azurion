package com.azurion.saascore.tributacion.application.dto;

import java.math.BigDecimal;

public record TaxResolution(
        String tipoOperacionCodigo,
        String tipoAfectacionCodigo,
        String tributoCodigo,
        BigDecimal porcentajeIgv,
        String moneda,
        String origen
) {
}
