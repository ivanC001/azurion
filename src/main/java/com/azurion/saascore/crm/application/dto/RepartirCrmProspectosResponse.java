package com.azurion.saascore.crm.application.dto;

import java.util.List;
import java.util.Map;

public record RepartirCrmProspectosResponse(
        int totalAsignados,
        Map<String, Long> asignadosPorResponsable,
        List<CrmProspectoResponse> prospectos
) {
}
