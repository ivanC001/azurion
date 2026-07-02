package com.azurion.saascore.facturacion.application.mappers;

import com.azurion.saascore.facturacion.application.dto.NotaFiscalResponse;
import com.azurion.saascore.facturacion.domain.entities.NotaFiscal;

public final class NotaFiscalMapper {

    private NotaFiscalMapper() {
    }

    public static NotaFiscalResponse toResponse(NotaFiscal nota) {
        return new NotaFiscalResponse(
                nota.getId(),
                nota.getExternalId(),
                nota.getTipoDocumento(),
                nota.getTipoNota(),
                nota.getVentaId(),
                nota.getVentaExternalId(),
                nota.getVentaTipoDocumento(),
                nota.getVentaNumeroDocumento(),
                nota.getClienteDocumento(),
                nota.getClienteNombre(),
                nota.getMoneda(),
                nota.getMonto(),
                nota.getFechaEmision(),
                nota.getMotivoCodigo(),
                nota.getMotivoDescripcion(),
                nota.getResponsableId(),
                nota.getResponsableNombre(),
                nota.getFacturacionEstado(),
                nota.getFacturacionIntentos(),
                nota.getFacturadorHttpStatus(),
                nota.getFacturadorEndpoint(),
                nota.getFacturadorTipoComprobante(),
                nota.getFacturadorMensaje(),
                nota.getFacturadorSunatEstado(),
                nota.getFacturadorDocumentoId(),
                nota.getFacturadorTicket(),
                nota.getFacturadorPdfUrl(),
                nota.getFacturadorXmlUrl(),
                nota.getFacturadorCdrUrl(),
                nota.getFacturadorRespuestaJson(),
                nota.getFacturacionActualizadoEn()
        );
    }
}
