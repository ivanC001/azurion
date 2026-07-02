package com.azurion.saascore.crm.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record CrmDashboardResponse(
        long prospectosNuevos,
        long prospectosConvertidos,
        long oportunidadesAbiertas,
        long oportunidadesGanadas,
        long oportunidadesPerdidas,
        long actividadesPendientes,
        long actividadesVencidas,
        long leadsAutomaticos,
        long leadsManuales,
        BigDecimal montoPipeline,
        List<CrmEtapaResumenResponse> embudo
) {
}
