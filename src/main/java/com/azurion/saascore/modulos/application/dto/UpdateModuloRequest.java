package com.azurion.saascore.modulos.application.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateModuloRequest(
        @NotBlank String nombre,
        String descripcion,
        @NotBlank String estado
) {
}
