package com.azurion.saascore.clientes.application.usecases;

import com.azurion.saascore.clientes.application.dto.ClienteResponse;
import com.azurion.saascore.clientes.domain.repositories.ClienteRepository;
import com.azurion.saascore.clientes.application.mappers.ClienteMapper;
import com.azurion.shared.api.PageResponse;
import com.azurion.shared.api.PageRequestSupport;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListClientesUseCase {

    private final ClienteRepository clienteRepository;

    @Transactional(readOnly = true)
    public List<ClienteResponse> execute() {
        return page("", 0, PageRequestSupport.MAX_SIZE).content();
    }

    @Transactional(readOnly = true)
    public PageResponse<ClienteResponse> page(String query, int page, int size) {
        var result = clienteRepository.search(
                query == null ? "" : query.trim(),
                PageRequestSupport.of(page, size, Sort.by(Sort.Direction.DESC, "id"))
        );
        return PageResponse.from(result, result.getContent().stream().map(ClienteMapper::toResponse).toList());
    }
}
