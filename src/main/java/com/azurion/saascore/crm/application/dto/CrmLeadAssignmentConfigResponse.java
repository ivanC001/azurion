package com.azurion.saascore.crm.application.dto;

import java.util.List;

public record CrmLeadAssignmentConfigResponse(
        boolean automatico,
        String estrategia,
        List<String> responsableIds
) {
}
