package com.azurion.saascore.suscripciones.application.usecases;

import com.azurion.saascore.suscripciones.application.dto.SuscripcionResponse;
import com.azurion.saascore.suscripciones.application.mappers.SuscripcionMapper;
import com.azurion.saascore.suscripciones.domain.repositories.SuscripcionRepository;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetSuscripcionByIdUseCase {

    private final SuscripcionRepository suscripcionRepository;

    @Transactional(readOnly = true)
    public SuscripcionResponse execute(Long id) {
        return suscripcionRepository.findById(id)
                .map(SuscripcionMapper::toResponse)
                .orElseThrow(() -> new BusinessException("SUSCRIPCION_NOT_FOUND", "Suscripcion not found: " + id));
    }
}
