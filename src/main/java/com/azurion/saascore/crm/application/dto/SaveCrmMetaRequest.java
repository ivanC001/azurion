package com.azurion.saascore.crm.application.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record SaveCrmMetaRequest(
        @NotNull @Min(2020) @Max(2100) Integer anio,
        @NotNull @Min(1) @Max(12) Integer mes,
        @NotBlank @Size(max = 20) String alcance,
        @Size(max = 80) String responsableId,
        @NotNull @DecimalMin("0.00") BigDecimal metaIngresos,
        @NotNull @Min(0) Integer metaOportunidadesGanadas,
        @NotNull @Min(0) Integer metaProspectosNuevos,
        @NotNull @Min(0) Integer metaActividadesRealizadas,
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal metaConversion
) {
}
