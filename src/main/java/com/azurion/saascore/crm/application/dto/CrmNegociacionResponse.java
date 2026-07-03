package com.azurion.saascore.crm.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CrmNegociacionResponse(
        Long id,
        Long oportunidadId,
        Long cotizacionId,
        String codigoCotizacion,
        String estado,
        String solicitudCliente,
        BigDecimal precioOriginal,
        BigDecimal descuento,
        BigDecimal precioFinal,
        String formaPago,
        Integer cuotas,
        LocalDate fechaInicio,
        LocalDate fechaEntrega,
        String observacion,
        String resultado,
        String usuarioId,
        String usuarioNombre,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
