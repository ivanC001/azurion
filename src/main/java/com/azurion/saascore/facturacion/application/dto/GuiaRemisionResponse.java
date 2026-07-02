package com.azurion.saascore.facturacion.application.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record GuiaRemisionResponse(
        Long id,
        String externalId,
        Long sucursalOrigenId,
        String sucursalOrigenNombre,
        Long sucursalDestinoId,
        String sucursalDestinoNombre,
        LocalDate fechaEmision,
        LocalDate fechaTraslado,
        String motivoTraslado,
        String transportista,
        String observacion,
        String responsableId,
        String responsableNombre,
        String itemsResumen,
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
