package com.azurion.saascore.crm.application.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateCrmLeadAssignmentConfigRequest(
        @NotNull Boolean automatico,
        List<String> responsableIds
) {
}
