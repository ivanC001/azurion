package com.azurion.saascore.crm.application.dto;

import java.util.List;

public record CrmReportesResponse(
        List<CrmEtapaResumenResponse> oportunidadesPorEtapa,
        long actividadesPendientes,
        long actividadesRealizadas,
        long prospectosConvertidos,
        long prospectosDescartados
) {
}
