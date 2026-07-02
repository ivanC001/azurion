package com.azurion.saascore.caja.application.usecases;

import com.azurion.saascore.caja.application.dto.CajaMovimientoResponse;
import com.azurion.saascore.auth.application.services.AuthorizationService;
import com.azurion.saascore.caja.application.dto.DepositoCuentaEmpresarialRequest;
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
public class DepositarCuentaEmpresarialUseCase {

    private final CajaRepository cajaRepository;
    private final CajaMovimientoService cajaMovimientoService;
    private final AuthorizationService authorizationService;

    @Transactional
    public CajaMovimientoResponse execute(Long cajaId, DepositoCuentaEmpresarialRequest request) {
        authorizationService.validarCaja(authorizationService.currentUsuarioId(), cajaId);
        Caja caja = cajaRepository.findById(cajaId)
                .orElseThrow(() -> new BusinessException("CAJA_NO_ENCONTRADA", "Caja no encontrada"));

        String descripcion = request.observacion() == null || request.observacion().isBlank()
                ? "Deposito a cuenta empresarial"
                : request.observacion();

        CajaMovimiento movimiento = cajaMovimientoService.registrar(
                caja,
                "DEPOSITO_CUENTA",
                request.monto(),
                descripcion,
                request.numeroOperacion(),
                request.cuentaEmpresarial(),
                request.responsableId(),
                request.responsableNombre()
        );
        cajaRepository.save(caja);
        return CajaMapper.toMovimientoResponse(movimiento);
    }
}
