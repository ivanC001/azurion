package com.azurion.saascore.planes.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record PlanResponse(
        Long id,
        String nombre,
        String codigo,
        String descripcion,
        Long limiteMensualBolsa,
        BigDecimal precioMensual,
        String estado,
        List<String> moduloCodigos
) {
}
