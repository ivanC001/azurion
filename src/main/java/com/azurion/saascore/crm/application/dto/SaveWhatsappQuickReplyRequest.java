package com.azurion.saascore.crm.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveWhatsappQuickReplyRequest(
        @NotBlank @Size(max = 80) String titulo,
        @NotBlank @Size(max = 4096) String mensaje
) {
}
