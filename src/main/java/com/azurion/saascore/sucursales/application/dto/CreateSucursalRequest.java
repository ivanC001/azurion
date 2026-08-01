package com.azurion.saascore.sucursales.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateSucursalRequest(
        @Size(max = 50) String codigo,
        @NotBlank String nombre,
        String direccion,
        @NotBlank String ubigeoCodigo,
        @NotNull BigDecimal igvPorcentaje,
        Boolean crearAlmacenPrincipal
) {
}
