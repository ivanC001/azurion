package com.azurion.saascore.sucursales.application.dto;

import java.math.BigDecimal;

public record SucursalResponse(
        Long id,
        String codigo,
        String nombre,
        String direccion,
        String ubigeoCodigo,
        String departamento,
        String provincia,
        String distrito,
        BigDecimal igvPorcentaje,
        String tipoOperacionDefaultId,
        String tipoAfectacionDefaultId,
        String tributoDefaultId,
        BigDecimal porcentajeIgvDefault,
        boolean activo
) {
}
