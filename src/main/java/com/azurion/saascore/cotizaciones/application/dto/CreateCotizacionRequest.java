package com.azurion.saascore.cotizaciones.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record CreateCotizacionRequest(
        Long clienteId,
        @NotBlank String usuarioId,
        @NotBlank String usuarioNombre,
        @NotNull Long sucursalId,
        LocalDate fechaEmision,
        LocalDate fechaVencimiento,
        String moneda,
        String observacion,
        Long crmOportunidadId,
        @NotEmpty List<@Valid CotizacionDetalleRequest> detalles
) {
}
