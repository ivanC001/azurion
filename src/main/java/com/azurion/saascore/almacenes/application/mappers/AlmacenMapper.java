package com.azurion.saascore.almacenes.application.mappers;

import com.azurion.saascore.almacenes.application.dto.AlmacenResponse;
import com.azurion.saascore.almacenes.domain.entities.Almacen;

public final class AlmacenMapper {

    private AlmacenMapper() {
    }

    public static AlmacenResponse toResponse(Almacen almacen) {
        return new AlmacenResponse(
                almacen.getId(),
                almacen.getCodigo(),
                almacen.getNombre(),
                almacen.getDireccion(),
                almacen.getSucursal().getId(),
                almacen.getSucursal().getCodigo(),
                almacen.getSucursal().getNombre(),
                almacen.getTipoAlmacen(),
                almacen.isPermiteVenta(),
                almacen.getEstado(),
                almacen.isActivo()
        );
    }
}
