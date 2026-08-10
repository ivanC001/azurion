package com.azurion.saascore.auth.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeCurrentUserPasswordRequest(
        @NotBlank @Size(max = 120) String contrasenaActual,
        @NotBlank @Size(min = 8, max = 120) String nuevaContrasena
) {
}
