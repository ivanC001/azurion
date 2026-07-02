package com.azurion.saascore.roles.application.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateRolRequest(
        @NotBlank String nombre,
        String descripcion,
        Boolean activo
) {
}
