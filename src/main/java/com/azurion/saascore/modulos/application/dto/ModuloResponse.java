package com.azurion.saascore.modulos.application.dto;

public record ModuloResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        String estado
) {
}
