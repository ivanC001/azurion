package com.azurion.saascore.usuarios.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUsuarioPasswordRequest(
        @NotBlank @Size(min = 8, max = 120) String password
) {
}
