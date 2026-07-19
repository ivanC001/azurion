package com.azurion.saascore.crm.application.dto;

import jakarta.validation.constraints.Size;

public record UpdateWhatsappConversationNoteRequest(
        @Size(max = 4000) String nota
) {
}
