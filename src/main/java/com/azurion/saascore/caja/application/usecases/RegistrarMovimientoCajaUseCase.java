package com.azurion.saascore.caja.application.usecases;

import com.azurion.saascore.caja.application.dto.CajaMovimientoResponse;
import com.azurion.saascore.auth.application.services.AuthorizationService;
import com.azurion.saascore.caja.application.dto.RegistrarMovimientoCajaRequest;
import com.azurion.saascore.caja.application.mappers.CajaMapper;
import com.azurion.saascore.caja.domain.entities.Caja;
import com.azurion.saascore.caja.domain.entities.CajaMovimiento;
import com.azurion.saascore.caja.domain.repositories.CajaRepository;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrarMovimientoCajaUseCase {

    private final CajaRepository cajaRepository;
    private final CajaMovimientoService cajaMovimientoService;
    private final AuthorizationService authorizationService;

    @Transactional
    public CajaMovimientoResponse execute(Long cajaId, RegistrarMovimientoCajaRequest request) {
        authorizationService.validarCaja(authorizationService.currentUsuarioId(), cajaId);
        Caja caja = cajaRepository.findById(cajaId)
                .orElseThrow(() -> new BusinessException("CAJA_NO_ENCONTRADA", "Caja no encontrada"));

        CajaMovimiento movimiento = cajaMovimientoService.registrar(
                caja,
                request.tipoMovimiento(),
                request.monto(),
                request.descripcion(),
                request.referencia(),
                null,
                request.responsableId(),
                request.responsableNombre()
        );
        cajaRepository.save(caja);
        return CajaMapper.toMovimientoResponse(movimiento);
    }
}
