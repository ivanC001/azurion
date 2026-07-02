package com.azurion.saascore.suscripciones.application.mappers;

import com.azurion.saascore.suscripciones.application.dto.SuscripcionResponse;
import com.azurion.saascore.suscripciones.domain.entities.Suscripcion;

public final class SuscripcionMapper {

    private SuscripcionMapper() {
    }

    public static SuscripcionResponse toResponse(Suscripcion suscripcion) {
        return new SuscripcionResponse(
                suscripcion.getId(),
                suscripcion.getEmpresa().getId(),
                suscripcion.getPlan().getId(),
                suscripcion.getEstado(),
                suscripcion.getFechaInicio(),
                suscripcion.getFechaFin()
        );
    }
}
