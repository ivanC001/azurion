package com.azurion.saascore.facturacion.application.usecases;

import com.azurion.saascore.facturacion.application.dto.NotaFiscalResponse;
import com.azurion.saascore.facturacion.application.mappers.NotaFiscalMapper;
import com.azurion.saascore.facturacion.domain.entities.NotaFiscal;
import com.azurion.saascore.facturacion.domain.repositories.NotaFiscalRepository;
import com.azurion.shared.api.PageRequestSupport;
import com.azurion.shared.api.PageResponse;
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
        return page(tipoDocumento, query, 0, PageRequestSupport.MAX_SIZE).content();
    }

    @Transactional(readOnly = true)
    public PageResponse<NotaFiscalResponse> page(String tipoDocumento, String query, int page, int size) {
        var result = notaFiscalRepository.search(
                normalizeTipo(tipoDocumento),
                query == null ? "" : query.trim(),
                PageRequestSupport.of(page, size, Sort.by(Sort.Direction.DESC, "id"))
        );
        return PageResponse.from(result, result.getContent().stream().map(NotaFiscalMapper::toResponse).toList());
    }

    private String normalizeTipo(String tipoDocumento) {
        if ("08".equals(tipoDocumento)) {
            return NotaFiscal.TIPO_DOCUMENTO_DEBITO;
        }
        return NotaFiscal.TIPO_DOCUMENTO_CREDITO;
    }
}
