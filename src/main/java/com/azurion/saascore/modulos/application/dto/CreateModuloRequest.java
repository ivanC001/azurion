package com.azurion.saascore.modulos.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateModuloRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Z0-9_]{3,60}$", message = "codigo must match ^[A-Z0-9_]{3,60}$")
        String codigo,
        @NotBlank String nombre,
        String descripcion
) {
}
