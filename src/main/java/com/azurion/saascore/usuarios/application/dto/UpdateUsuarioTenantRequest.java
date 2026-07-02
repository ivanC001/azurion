package com.azurion.saascore.usuarios.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateUsuarioTenantRequest(
        @NotBlank @Size(max = 160) String nombres,
        @Email @Size(max = 180) String email,
        Boolean activo,
        List<Long> sucursalIds
) {
}
