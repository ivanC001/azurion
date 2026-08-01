package com.azurion.saascore.caja.application.dto;

import java.util.List;

public record CajaFisicaResponse(
        Long id,
        Long sucursalId,
        String sucursalCodigo,
        String sucursalNombre,
        String codigo,
        String nombre,
        String moneda,
        String estado,
        List<Long> usuarioIds
) {
}
