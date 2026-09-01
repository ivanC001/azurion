package com.azurion.saascore.sucursales.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateSucursalRequest(
        @NotBlank String codigo,
        @NotBlank String nombre,
        String direccion,
        /** Codigo SUNAT. Obligatorio solo para tenants de Peru. */
        String ubigeoCodigo,
        /** Ubicacion libre para tenants de otro pais; si falta se hereda de la empresa. */
        String departamento,
        String provincia,
        String distrito,
        @NotNull BigDecimal igvPorcentaje
) {
}
