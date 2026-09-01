package com.azurion.saascore.crm.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Programa un reenganche llenando las variables desde una cotizacion del prospecto.
 *
 * @param cotizacionId cotizacion a citar; si va vacio se toma la ultima que el cliente
 *                     llego a ver (estado ENVIADA o VENCIDA)
 * @param campos       que valor va en cada variable, en orden; si va vacio se usa
 *                     NOMBRE, COTIZACION, PRODUCTO, TOTAL, VENCIMIENTO
 */
public record ScheduleQuoteReengagementRequest(
        @NotBlank @Size(max = 512) String nombre,
        @NotBlank @Size(max = 35) String idioma,
        @NotNull LocalDateTime programadoPara,
        Long cotizacionId,
        @Size(max = 30) List<CotizacionReengagementField> campos
) {
}
