package com.azurion.saascore.configuracion.application.dto;

import java.time.LocalDate;

public record EmpresaModuloResponse(
        Long id,
        Long empresaId,
        Long moduloId,
        String moduloCodigo,
        String moduloNombre,
        String estado,
        boolean activo,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String configuracionExtra,
        boolean vigente
) {
}
