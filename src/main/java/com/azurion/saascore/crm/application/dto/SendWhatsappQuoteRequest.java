package com.azurion.saascore.crm.application.dto;

import jakarta.validation.constraints.Size;

public record SendWhatsappQuoteRequest(
        @Size(max = 1024)
        String mensaje
) {
}
