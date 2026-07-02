package com.azurion.saascore.ventas.application.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record VentaResponse(
        Long id,
        String externalId,
        String clienteDocumento,
        String clienteNombre,
        String moneda,
        BigDecimal total,
        OffsetDateTime fechaVenta,
        String facturacionEstado,
        Integer facturacionIntentos,
        Integer facturadorHttpStatus,
        String facturadorEndpoint,
        String facturadorTipoComprobante,
        String facturadorMensaje,
        String facturadorSunatEstado,
        String facturadorDocumentoId,
        String facturadorTicket,
        String facturadorPdfUrl,
        String facturadorXmlUrl,
        String facturadorCdrUrl,
        String facturadorRespuestaJson,
        OffsetDateTime facturacionActualizadoEn
) {
}
