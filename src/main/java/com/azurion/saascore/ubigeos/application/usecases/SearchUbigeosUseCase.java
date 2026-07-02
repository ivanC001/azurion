package com.azurion.saascore.ubigeos.application.usecases;

import com.azurion.saascore.ubigeos.application.dto.UbigeoResponse;
import com.azurion.saascore.ubigeos.domain.repositories.UbigeoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchUbigeosUseCase {

    private final UbigeoRepository ubigeoRepository;

    @Transactional(readOnly = true)
    public List<UbigeoResponse> execute(String query) {
        String normalized = query == null || query.trim().isBlank() ? null : query.trim();
        return ubigeoRepository.search(normalized, PageRequest.of(0, 50)).stream()
                .map(ubigeo -> new UbigeoResponse(
                        ubigeo.getCodigo(),
                        ubigeo.getDepartamento(),
                        ubigeo.getProvincia(),
                        ubigeo.getDistrito()
                ))
                .toList();
    }
}
