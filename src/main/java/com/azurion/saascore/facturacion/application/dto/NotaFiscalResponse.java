package com.azurion.saascore.facturacion.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record NotaFiscalResponse(
        Long id,
        String externalId,
        String tipoDocumento,
        String tipoNota,
        Long ventaId,
        String ventaExternalId,
        String ventaTipoDocumento,
        String ventaNumeroDocumento,
        String clienteDocumento,
        String clienteNombre,
        String moneda,
        BigDecimal monto,
        LocalDate fechaEmision,
        String motivoCodigo,
        String motivoDescripcion,
        String responsableId,
        String responsableNombre,
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
