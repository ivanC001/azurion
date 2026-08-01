package com.azurion.saascore.caja.application.usecases;

import com.azurion.saascore.caja.application.dto.CajaMovimientoResponse;
import com.azurion.saascore.caja.application.mappers.CajaMapper;
import com.azurion.saascore.caja.domain.repositories.CajaMovimientoRepository;
import com.azurion.saascore.caja.application.services.CajaTurnoService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListCajaMovimientosUseCase {

    private final CajaMovimientoRepository cajaMovimientoRepository;
    private final CajaTurnoService cajaTurnoService;

    public List<CajaMovimientoResponse> execute(Long turnoId) {
        var turno = cajaTurnoService.find(turnoId);
        cajaTurnoService.requireAccess(turno, false);
        return cajaMovimientoRepository.findByTurnoIdOrderByFechaMovimientoDesc(turnoId).stream()
                .map(CajaMapper::toMovimientoResponse)
                .toList();
    }
}
