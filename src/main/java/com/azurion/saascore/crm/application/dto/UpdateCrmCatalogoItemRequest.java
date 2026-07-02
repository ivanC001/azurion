package com.azurion.saascore.crm.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateCrmCatalogoItemRequest(
        String tipoItem,
        @Size(max = 220) String nombre,
        @Size(max = 1500) String descripcion,
        @DecimalMin("0.00") BigDecimal precioReferencial,
        String estado,
        String metadataJson,
        Boolean publicEnabled,
        @Size(max = 140) String landingSlug
) {
}
