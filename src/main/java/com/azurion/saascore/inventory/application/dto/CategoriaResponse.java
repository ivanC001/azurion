package com.azurion.saascore.inventory.application.dto;

public record CategoriaResponse(
        Long id,
        String nombre,
        String descripcion,
        Long padreId,
        String estado
) {
}
