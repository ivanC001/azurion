package com.azurion.saascore.crm.application.dto;

import java.math.BigDecimal;

public record PublicCrmCatalogoItemResponse(
        Long id,
        String tipoItem,
        String nombre,
        String descripcion,
        BigDecimal precioReferencial,
        String metadataJson
) {
}
