package com.azurion.saascore.caja.application.usecases;

import com.azurion.saascore.caja.application.dto.CajaResponse;
import com.azurion.saascore.caja.application.mappers.CajaMapper;
import com.azurion.saascore.caja.domain.repositories.CajaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListCajasUseCase {

    private final CajaRepository cajaRepository;

    public List<CajaResponse> execute(String estado, Long sucursalId) {
        if (sucursalId == null && (estado == null || estado.isBlank())) {
            return cajaRepository.findAllByOrderByFechaAperturaDesc().stream()
                    .map(CajaMapper::toResponse)
                    .toList();
        }

        if (sucursalId == null) {
            return cajaRepository.findByEstadoOrderByFechaAperturaDesc(estado.trim().toUpperCase()).stream()
                    .map(CajaMapper::toResponse)
                    .toList();
        }

        if (estado == null || estado.isBlank()) {
            return cajaRepository.findBySucursalIdOrderByFechaAperturaDesc(sucursalId).stream()
                    .map(CajaMapper::toResponse)
                    .toList();
        }

        return cajaRepository.findBySucursalIdAndEstadoOrderByFechaAperturaDesc(sucursalId, estado.trim().toUpperCase()).stream()
                .map(CajaMapper::toResponse)
                .toList();
    }
}
