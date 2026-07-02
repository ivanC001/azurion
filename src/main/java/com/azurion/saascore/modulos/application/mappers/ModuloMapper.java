package com.azurion.saascore.modulos.application.mappers;

import com.azurion.saascore.modulos.application.dto.ModuloResponse;
import com.azurion.saascore.modulos.domain.entities.Modulo;

public final class ModuloMapper {

    private ModuloMapper() {
    }

    public static ModuloResponse toResponse(Modulo modulo) {
        return new ModuloResponse(
                modulo.getId(),
                modulo.getCodigo(),
                modulo.getNombre(),
                modulo.getDescripcion(),
                modulo.getEstado()
        );
    }
}
