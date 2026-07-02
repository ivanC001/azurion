package com.azurion.saascore.usuarios.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignUsuarioTenantRolRequest(
        @NotBlank String rolCodigo
) {
}
