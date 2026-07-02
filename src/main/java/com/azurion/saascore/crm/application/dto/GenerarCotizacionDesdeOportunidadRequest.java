package com.azurion.saascore.crm.application.dto;

import com.azurion.saascore.cotizaciones.application.dto.CotizacionDetalleRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record GenerarCotizacionDesdeOportunidadRequest(
        Long clienteId,
        @NotBlank String usuarioId,
        @NotBlank String usuarioNombre,
        @NotNull Long sucursalId,
        LocalDate fechaEmision,
        LocalDate fechaVencimiento,
        String moneda,
        String observacion,
        @NotEmpty List<@Valid CotizacionDetalleRequest> detalles
) {
}
