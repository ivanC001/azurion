package com.azurion.saascore.suscripciones.application.mappers;

import com.azurion.saascore.suscripciones.application.dto.SuscripcionResponse;
import com.azurion.saascore.suscripciones.domain.entities.Suscripcion;

public final class SuscripcionMapper {

    private SuscripcionMapper() {
    }

    public static SuscripcionResponse toResponse(Suscripcion suscripcion) {
        Integer limitePlan = suscripcion.getPlan().getLimiteUsuarios();
        Integer limitePersonalizado = suscripcion.getLimiteUsuarios();
        return new SuscripcionResponse(
                suscripcion.getId(),
                suscripcion.getEmpresa().getId(),
                suscripcion.getPlan().getId(),
                suscripcion.getPlan().getNombre(),
                suscripcion.getPlan().getCodigo(),
                limitePlan,
                limitePersonalizado == null ? limitePlan : limitePersonalizado,
                limitePersonalizado != null,
                suscripcion.getEstado(),
                suscripcion.getFechaInicio(),
                suscripcion.getFechaFin()
        );
    }
}
