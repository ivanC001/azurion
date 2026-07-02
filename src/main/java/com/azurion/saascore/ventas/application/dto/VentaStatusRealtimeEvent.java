package com.azurion.saascore.ventas.application.dto;

import com.azurion.saascore.ventas.domain.entities.Venta;
import java.time.OffsetDateTime;

public record VentaStatusRealtimeEvent(
        String tenantId,
        String source,
        Long ventaId,
        String externalId,
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
        OffsetDateTime facturacionActualizadoEn
) {
    public static VentaStatusRealtimeEvent fromVenta(String tenantId, String source, Venta venta) {
        return new VentaStatusRealtimeEvent(
                tenantId,
                source,
                venta.getId(),
                venta.getExternalId(),
                venta.getFacturacionEstado(),
                venta.getFacturacionIntentos(),
                venta.getFacturadorHttpStatus(),
                venta.getFacturadorEndpoint(),
                venta.getFacturadorTipoComprobante(),
                venta.getFacturadorMensaje(),
                venta.getFacturadorSunatEstado(),
                venta.getFacturadorDocumentoId(),
                venta.getFacturadorTicket(),
                venta.getFacturadorPdfUrl(),
                venta.getFacturadorXmlUrl(),
                venta.getFacturadorCdrUrl(),
                venta.getFacturacionActualizadoEn()
        );
    }
}
