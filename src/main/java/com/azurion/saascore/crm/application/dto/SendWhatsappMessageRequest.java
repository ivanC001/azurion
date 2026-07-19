package com.azurion.saascore.crm.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendWhatsappMessageRequest(
        @NotBlank @Size(max = 4096) String mensaje,
        Boolean previewUrl
) {
}
