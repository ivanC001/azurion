package com.azurion.saascore.caja.application.usecases;

import com.azurion.saascore.caja.application.dto.CajaResponse;
import com.azurion.saascore.caja.application.mappers.CajaMapper;
import com.azurion.saascore.caja.domain.repositories.CajaRepository;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetCajaByIdUseCase {

    private final CajaRepository cajaRepository;

    public CajaResponse execute(Long cajaId) {
        return cajaRepository.findById(cajaId)
                .map(CajaMapper::toResponse)
                .orElseThrow(() -> new BusinessException("CAJA_NO_ENCONTRADA", "Caja no encontrada"));
    }
}
