package com.azurion.saascore.crm.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveWhatsappConversationNoteRequest(
        @NotBlank
        @Size(max = 4000)
        String nota
) {
}
