package com.azurion.saascore.crm.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MarcarPerdidaRequest(
        @NotBlank @Size(max = 500) String motivo
) {
}
