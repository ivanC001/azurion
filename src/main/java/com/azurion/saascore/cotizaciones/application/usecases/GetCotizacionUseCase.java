package com.azurion.saascore.cotizaciones.application.usecases;

import com.azurion.saascore.cotizaciones.application.dto.CotizacionResponse;
import com.azurion.saascore.cotizaciones.application.mappers.CotizacionMapper;
import com.azurion.saascore.cotizaciones.domain.entities.Cotizacion;
import com.azurion.saascore.cotizaciones.domain.repositories.CotizacionRepository;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetCotizacionUseCase {

    private final CotizacionRepository cotizacionRepository;

    @Transactional(readOnly = true)
    public CotizacionResponse execute(Long id) {
        return CotizacionMapper.toResponse(find(id));
    }

    Cotizacion find(Long id) {
        return cotizacionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("COTIZACION_NO_ENCONTRADA", "Cotizacion no encontrada"));
    }
}
