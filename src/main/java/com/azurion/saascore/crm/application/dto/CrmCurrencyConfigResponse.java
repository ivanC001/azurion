package com.azurion.saascore.crm.application.dto;

import java.math.BigDecimal;

public record CrmCurrencyConfigResponse(
        Long id,
        String moneda,
        String nombre,
        String simbolo,
        BigDecimal tipoCambioBase,
        BigDecimal margenConversionPorcentaje,
        BigDecimal tipoCambioVenta,
        Boolean activo
) {
}
