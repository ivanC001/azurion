package com.azurion.saascore.configuracion.application.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AssignEmpresaModuloRequest(
        @NotNull Long moduloId,
        @NotNull Boolean activo,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String configuracionExtra
) {
}
