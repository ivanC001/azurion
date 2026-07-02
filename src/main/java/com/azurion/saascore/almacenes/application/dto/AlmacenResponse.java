package com.azurion.saascore.almacenes.application.dto;

public record AlmacenResponse(
        Long id,
        String codigo,
        String nombre,
        String direccion,
        Long sucursalId,
        String sucursalCodigo,
        String sucursalNombre,
        String tipoAlmacen,
        String estado,
        boolean activo
) {
}
