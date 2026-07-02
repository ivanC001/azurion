package com.azurion.saascore.usuarios.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateUsuarioTenantRequest(
        @NotBlank @Size(min = 3, max = 120) String username,
        @NotBlank @Size(min = 8, max = 120) String password,
        @NotBlank @Size(max = 160) String nombres,
        @Email @Size(max = 180) String email,
        List<String> rolCodigos,
        List<Long> sucursalIds
) {
}
