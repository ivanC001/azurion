package com.azurion.saascore.suscripciones.application.usecases;

import com.azurion.saascore.suscripciones.application.dto.SuscripcionResponse;
import com.azurion.saascore.suscripciones.application.mappers.SuscripcionMapper;
import com.azurion.saascore.suscripciones.domain.repositories.SuscripcionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListSuscripcionesUseCase {

    private final SuscripcionRepository suscripcionRepository;

    public List<SuscripcionResponse> execute(Long empresaId) {
        if (empresaId == null) {
            return suscripcionRepository.findAll().stream()
                    .map(SuscripcionMapper::toResponse)
                    .toList();
        }

        return suscripcionRepository.findByEmpresaId(empresaId).stream()
                .map(SuscripcionMapper::toResponse)
                .toList();
    }
}
