package com.azurion.saascore.crm.application.dto;

import java.math.BigDecimal;

public record CrmReporteBucketResponse(
        String codigo,
        String nombre,
        long cantidad,
        BigDecimal monto
) {
}
