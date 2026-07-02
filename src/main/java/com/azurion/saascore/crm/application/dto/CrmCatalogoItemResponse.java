package com.azurion.saascore.crm.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CrmCatalogoItemResponse(
        Long id,
        String tipoItem,
        String nombre,
        String descripcion,
        BigDecimal precioReferencial,
        String estado,
        String metadataJson,
        String publicToken,
        Boolean publicEnabled,
        String landingSlug,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
