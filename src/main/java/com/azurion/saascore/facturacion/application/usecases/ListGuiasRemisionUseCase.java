package com.azurion.saascore.facturacion.application.usecases;

import com.azurion.saascore.facturacion.application.dto.GuiaRemisionResponse;
import com.azurion.saascore.facturacion.domain.entities.GuiaRemision;
import com.azurion.saascore.facturacion.domain.repositories.GuiaRemisionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListGuiasRemisionUseCase {

    private final GuiaRemisionRepository guiaRemisionRepository;

    @Transactional(readOnly = true)
    public List<GuiaRemisionResponse> execute(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase();
        return guiaRemisionRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .filter(guia -> normalized.isBlank() || matches(guia, normalized))
                .map(this::toResponse)
                .toList();
    }

    private boolean matches(GuiaRemision guia, String query) {
        return contains(guia.getExternalId(), query)
                || contains(guia.getSucursalOrigenNombre(), query)
                || contains(guia.getSucursalDestinoNombre(), query)
                || contains(guia.getMotivoTraslado(), query)
                || contains(guia.getTransportista(), query)
                || contains(guia.getResponsableNombre(), query)
                || contains(guia.getFacturacionEstado(), query)
                || contains(guia.getFacturadorSunatEstado(), query)
                || contains(guia.getFacturadorMensaje(), query);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    public GuiaRemisionResponse toResponse(GuiaRemision guia) {
        return new GuiaRemisionResponse(
                guia.getId(),
                guia.getExternalId(),
                guia.getSucursalOrigenId(),
                guia.getSucursalOrigenNombre(),
                guia.getSucursalDestinoId(),
                guia.getSucursalDestinoNombre(),
                guia.getFechaEmision(),
                guia.getFechaTraslado(),
                guia.getMotivoTraslado(),
                guia.getTransportista(),
                guia.getObservacion(),
                guia.getResponsableId(),
                guia.getResponsableNombre(),
                guia.getItemsResumen(),
                guia.getFacturacionEstado(),
                guia.getFacturacionIntentos(),
                guia.getFacturadorHttpStatus(),
                guia.getFacturadorEndpoint(),
                guia.getFacturadorTipoComprobante(),
                guia.getFacturadorMensaje(),
                guia.getFacturadorSunatEstado(),
                guia.getFacturadorDocumentoId(),
                guia.getFacturadorTicket(),
                guia.getFacturadorPdfUrl(),
                guia.getFacturadorXmlUrl(),
                guia.getFacturadorCdrUrl(),
                guia.getFacturadorRespuestaJson(),
                guia.getFacturacionActualizadoEn()
        );
    }
}
