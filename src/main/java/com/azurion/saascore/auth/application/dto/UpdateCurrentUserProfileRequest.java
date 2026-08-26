package com.azurion.saascore.auth.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCurrentUserProfileRequest(
        @NotBlank @Size(max = 160) String nombres,
        @Size(max = 160) String apellidos,
        @Size(max = 40) String telefono,
        @Size(max = 120) String cargo,
        @Email @Size(max = 180) String email
) {
}
