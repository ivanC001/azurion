package com.azurion.saascore.cotizaciones.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record CotizacionResponse(
        Long id,
        Long clienteId,
        String clienteDocumento,
        String clienteNombre,
        String usuarioId,
        String usuarioNombre,
        Long sucursalId,
        String sucursalCodigo,
        String sucursalNombre,
        LocalDate fechaEmision,
        LocalDate fechaVencimiento,
        String moneda,
        BigDecimal subtotal,
        BigDecimal total,
        String estado,
        String observacion,
        Long ventaId,
        Long crmOportunidadId,
        OffsetDateTime fechaEnvio,
        String canalEnvio,
        OffsetDateTime proximoSeguimientoEn,
        OffsetDateTime fechaRespuesta,
        String motivoRechazo,
        String decisionSiguiente,
        OffsetDateTime convertidaEn,
        List<CotizacionDetalleResponse> detalles
) {
}
