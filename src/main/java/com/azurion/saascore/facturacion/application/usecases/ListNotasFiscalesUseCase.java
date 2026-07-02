package com.azurion.saascore.facturacion.application.usecases;

import com.azurion.saascore.facturacion.application.dto.NotaFiscalResponse;
import com.azurion.saascore.facturacion.application.mappers.NotaFiscalMapper;
import com.azurion.saascore.facturacion.domain.entities.NotaFiscal;
import com.azurion.saascore.facturacion.domain.repositories.NotaFiscalRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListNotasFiscalesUseCase {

    private final NotaFiscalRepository notaFiscalRepository;

    @Transactional(readOnly = true)
    public List<NotaFiscalResponse> execute(String tipoDocumento, String query) {
        String tipo = normalizeTipo(tipoDocumento);
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();

        return notaFiscalRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .filter(nota -> tipo.equals(nota.getTipoDocumento()))
                .filter(nota -> normalizedQuery.isBlank() || matches(nota, normalizedQuery))
                .map(NotaFiscalMapper::toResponse)
                .toList();
    }

    private boolean matches(NotaFiscal nota, String query) {
        return contains(nota.getExternalId(), query)
                || contains(nota.getVentaExternalId(), query)
                || contains(nota.getVentaNumeroDocumento(), query)
                || contains(nota.getClienteNombre(), query)
                || contains(nota.getClienteDocumento(), query)
                || contains(nota.getMotivoDescripcion(), query)
                || contains(nota.getFacturacionEstado(), query)
                || contains(nota.getFacturadorSunatEstado(), query)
                || contains(nota.getFacturadorMensaje(), query);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private String normalizeTipo(String tipoDocumento) {
        if ("08".equals(tipoDocumento)) {
            return NotaFiscal.TIPO_DOCUMENTO_DEBITO;
        }
        return NotaFiscal.TIPO_DOCUMENTO_CREDITO;
    }
}
