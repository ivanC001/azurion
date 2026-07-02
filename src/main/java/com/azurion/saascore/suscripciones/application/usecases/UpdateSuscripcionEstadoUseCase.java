package com.azurion.saascore.suscripciones.application.usecases;

import com.azurion.saascore.suscripciones.application.dto.SuscripcionResponse;
import com.azurion.saascore.suscripciones.application.dto.UpdateSuscripcionEstadoRequest;
import com.azurion.saascore.suscripciones.application.mappers.SuscripcionMapper;
import com.azurion.saascore.suscripciones.domain.entities.Suscripcion;
import com.azurion.saascore.suscripciones.domain.repositories.SuscripcionRepository;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateSuscripcionEstadoUseCase {

    private final SuscripcionRepository suscripcionRepository;

    @Transactional
    public SuscripcionResponse execute(Long id, UpdateSuscripcionEstadoRequest request) {
        Suscripcion suscripcion = suscripcionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("SUSCRIPCION_NOT_FOUND", "Suscripcion not found: " + id));

        suscripcion.setEstado(request.estado().trim().toUpperCase());
        suscripcion.setFechaFin(request.fechaFin());
        return SuscripcionMapper.toResponse(suscripcionRepository.save(suscripcion));
    }
}
