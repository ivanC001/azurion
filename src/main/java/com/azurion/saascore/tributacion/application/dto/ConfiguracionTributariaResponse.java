package com.azurion.saascore.tributacion.application.dto;

import java.math.BigDecimal;

public record ConfiguracionTributariaResponse(
        Long id,
        String tipoOperacionDefaultId,
        String tipoAfectacionDefaultId,
        String tributoDefaultId,
        BigDecimal porcentajeIgvDefault,
        String monedaDefault,
        String estado
) {
}
