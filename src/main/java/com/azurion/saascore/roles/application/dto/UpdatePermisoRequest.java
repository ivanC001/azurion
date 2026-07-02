package com.azurion.saascore.roles.application.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdatePermisoRequest(
        @NotBlank String nombre,
        String descripcion,
        @NotBlank String modulo,
        Boolean activo
) {
}
