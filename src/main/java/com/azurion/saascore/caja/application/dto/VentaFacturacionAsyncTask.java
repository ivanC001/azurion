package com.azurion.saascore.caja.application.dto;

import java.util.Map;

public record VentaFacturacionAsyncTask(
        String tenantId,
        String tenantRuc,
        Long ventaId,
        String externalId,
        String endpoint,
        String tipoComprobante,
        Map<String, Object> payload
) {
}
