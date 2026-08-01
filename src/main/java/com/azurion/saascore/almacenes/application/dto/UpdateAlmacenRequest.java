package com.azurion.saascore.almacenes.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateAlmacenRequest(
        @NotBlank String nombre,
        String direccion,
        @NotNull Long sucursalId,
        String tipoAlmacen,
        Boolean permiteVenta,
        Boolean activo
) {
}
