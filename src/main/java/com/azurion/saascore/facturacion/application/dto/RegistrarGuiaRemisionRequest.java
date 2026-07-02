package com.azurion.saascore.facturacion.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

public record RegistrarGuiaRemisionRequest(
        @NotNull Long sucursalOrigenId,
        @NotNull Long sucursalDestinoId,
        @NotBlank String fechaTraslado,
        String motivoTraslado,
        String transportista,
        String observacion,
        @NotBlank String responsableId,
        @NotBlank String responsableNombre,
        @NotEmpty List<@Valid GuiaItemRequest> items
) {
    public record GuiaItemRequest(
            @NotNull Long productoId,
            String descripcion,
            @NotNull @Positive BigDecimal cantidad
    ) {
    }
}
