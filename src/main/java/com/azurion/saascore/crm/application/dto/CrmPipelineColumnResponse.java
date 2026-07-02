package com.azurion.saascore.crm.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record CrmPipelineColumnResponse(
        CrmEtapaPipelineResponse etapa,
        long cantidad,
        BigDecimal monto,
        List<CrmOportunidadResponse> oportunidades
) {
}
