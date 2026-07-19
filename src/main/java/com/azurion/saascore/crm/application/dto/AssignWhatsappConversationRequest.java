package com.azurion.saascore.crm.application.dto;

import jakarta.validation.constraints.Size;

public record AssignWhatsappConversationRequest(
        @Size(max = 80) String responsableId
) {
}
