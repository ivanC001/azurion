package com.azurion.saascore.crm.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record CrmOportunidadRecursoRequest(
        @NotBlank String tipo,
        @NotNull Map<String, Object> data
) {
}
