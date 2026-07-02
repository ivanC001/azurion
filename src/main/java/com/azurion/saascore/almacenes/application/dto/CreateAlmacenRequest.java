package com.azurion.saascore.almacenes.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAlmacenRequest(
        @NotBlank String codigo,
        @NotBlank String nombre,
        String direccion,
        @NotNull Long sucursalId
) {
}
