package com.azurion.saascore.facturacion.application.usecases;

import com.azurion.saascore.facturacion.application.dto.GuiaRemisionResponse;
import com.azurion.saascore.facturacion.domain.entities.GuiaRemision;
import com.azurion.saascore.facturacion.domain.repositories.GuiaRemisionRepository;
import com.azurion.shared.api.PageRequestSupport;
import com.azurion.shared.api.PageResponse;
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
        return page(query, 0, PageRequestSupport.MAX_SIZE).content();
    }

    @Transactional(readOnly = true)
    public PageResponse<GuiaRemisionResponse> page(String query, int page, int size) {
        var result = guiaRemisionRepository.search(
                query == null ? "" : query.trim(),
                PageRequestSupport.of(page, size, Sort.by(Sort.Direction.DESC, "id"))
        );
        return PageResponse.from(result, result.getContent().stream().map(this::toResponse).toList());
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
