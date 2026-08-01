package com.azurion.saascore.crm.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendCrmOpportunityEmailRequest(
        @NotBlank @Size(max = 220) String asunto,
        @NotBlank @Size(max = 4000) String mensaje
) {
}
