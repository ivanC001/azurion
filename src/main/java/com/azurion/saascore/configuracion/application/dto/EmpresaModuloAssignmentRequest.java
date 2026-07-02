package com.azurion.saascore.configuracion.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record EmpresaModuloAssignmentRequest(
        Long moduloId,
        String moduloCodigo,
        @NotBlank String estado,
        @NotNull Boolean activo,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String configuracionExtra
) {
}
