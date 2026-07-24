package com.azurion.saascore.auth.application.dto;

import jakarta.validation.constraints.NotBlank;

public record ReplaceSessionRequest(
        @NotBlank String replacementToken,
        @NotBlank String deviceId
) {
}
