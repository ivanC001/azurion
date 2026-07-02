package com.azurion.saascore.auth.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterAdminGeneralRequest(
        @NotBlank @Size(min = 3, max = 120) String username,
        @NotBlank @Size(min = 8, max = 120) String password
) {
}
