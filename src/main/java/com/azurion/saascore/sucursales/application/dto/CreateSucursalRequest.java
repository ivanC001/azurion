package com.azurion.saascore.sucursales.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateSucursalRequest(
        @NotBlank String codigo,
        @NotBlank String nombre,
        String direccion,
        @NotBlank String ubigeoCodigo,
        @NotNull BigDecimal igvPorcentaje
) {
}
