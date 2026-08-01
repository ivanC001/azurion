package com.azurion.saascore.almacenes.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAlmacenRequest(
        @Size(max = 50) String codigo,
        @Size(max = 150) String nombre,
        String direccion,
        @NotNull Long sucursalId,
        String tipoAlmacen,
        Boolean permiteVenta
) {
}
