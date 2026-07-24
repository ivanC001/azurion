package com.azurion.saascore.suscripciones.application.dto;

import java.time.LocalDate;

public record SuscripcionResponse(
        Long id,
        Long empresaId,
        Long planId,
        String planNombre,
        String planCodigo,
        Integer limiteUsuariosPlan,
        Integer limiteUsuarios,
        boolean limiteUsuariosPersonalizado,
        String estado,
        LocalDate fechaInicio,
        LocalDate fechaFin
) {
}
