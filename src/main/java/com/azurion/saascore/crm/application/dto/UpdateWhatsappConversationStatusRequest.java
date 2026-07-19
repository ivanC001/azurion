package com.azurion.saascore.crm.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateWhatsappConversationStatusRequest(
        @NotBlank
        @Pattern(regexp = "ABIERTA|RESUELTA|ARCHIVADA")
        String estado
) {
}
