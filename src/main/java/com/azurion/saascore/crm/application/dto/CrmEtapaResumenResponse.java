package com.azurion.saascore.crm.application.dto;

import java.math.BigDecimal;

public record CrmEtapaResumenResponse(
        String etapa,
        long cantidad,
        BigDecimal monto
) {
}
