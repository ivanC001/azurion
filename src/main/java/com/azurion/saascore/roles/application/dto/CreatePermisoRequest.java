package com.azurion.saascore.roles.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePermisoRequest(
        @NotBlank String codigo,
        @NotBlank String nombre,
        String descripcion,
        @NotBlank String modulo
) {
}
