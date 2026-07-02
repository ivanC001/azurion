package com.azurion.saascore.caja.application.usecases;

import com.azurion.saascore.caja.application.dto.CajaMovimientoResponse;
import com.azurion.saascore.caja.application.mappers.CajaMapper;
import com.azurion.saascore.caja.domain.repositories.CajaRepository;
import com.azurion.saascore.caja.domain.repositories.CajaMovimientoRepository;
import com.azurion.shared.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListCajaMovimientosUseCase {

    private final CajaRepository cajaRepository;
    private final CajaMovimientoRepository cajaMovimientoRepository;

    public List<CajaMovimientoResponse> execute(Long cajaId) {
        if (!cajaRepository.existsById(cajaId)) {
            throw new BusinessException("CAJA_NO_ENCONTRADA", "Caja no encontrada");
        }

        return cajaMovimientoRepository.findByCajaIdOrderByFechaMovimientoDesc(cajaId).stream()
                .map(CajaMapper::toMovimientoResponse)
                .toList();
    }
}
