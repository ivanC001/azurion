package com.azurion.saascore.roles.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRolRequest(
        @NotBlank String codigo,
        @NotBlank String nombre,
        String descripcion
) {
}
