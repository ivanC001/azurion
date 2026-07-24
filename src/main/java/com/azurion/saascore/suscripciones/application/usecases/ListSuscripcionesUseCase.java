package com.azurion.saascore.suscripciones.application.usecases;

import com.azurion.saascore.suscripciones.application.dto.SuscripcionResponse;
import com.azurion.saascore.suscripciones.application.mappers.SuscripcionMapper;
import com.azurion.saascore.suscripciones.domain.repositories.SuscripcionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListSuscripcionesUseCase {

    private final SuscripcionRepository suscripcionRepository;

    @Transactional(readOnly = true)
    public List<SuscripcionResponse> execute(Long empresaId) {
        if (empresaId == null) {
            return suscripcionRepository.findAllByOrderByIdDesc().stream()
                    .map(SuscripcionMapper::toResponse)
                    .toList();
        }

        return suscripcionRepository.findByEmpresaIdOrderByIdDesc(empresaId).stream()
                .map(SuscripcionMapper::toResponse)
                .toList();
    }
}
