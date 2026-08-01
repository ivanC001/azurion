package com.azurion.saascore.caja.application.usecases;

import com.azurion.saascore.caja.application.dto.CajaMovimientoResponse;
import com.azurion.saascore.caja.application.dto.RegistrarMovimientoCajaRequest;
import com.azurion.saascore.caja.application.mappers.CajaMapper;
import com.azurion.saascore.caja.domain.entities.CajaMovimiento;
import com.azurion.saascore.caja.domain.entities.CajaTurno;
import com.azurion.saascore.caja.domain.repositories.CajaTurnoRepository;
import com.azurion.saascore.caja.application.services.CajaTurnoService;
import com.azurion.saascore.caja.domain.repositories.CajaMovimientoRepository;
import com.azurion.shared.exception.BusinessException;
import com.azurion.shared.util.RequestFingerprint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrarMovimientoCajaUseCase {

    private final CajaTurnoRepository cajaTurnoRepository;
    private final CajaMovimientoService cajaMovimientoService;
    private final CajaTurnoService cajaTurnoService;
    private final CajaMovimientoRepository cajaMovimientoRepository;

    @Transactional
    public CajaMovimientoResponse execute(Long turnoId, RegistrarMovimientoCajaRequest request) {
        CajaTurno turno = cajaTurnoService.findForUpdate(turnoId);
        cajaTurnoService.requireAccess(turno, true);
        String operationKey = normalizeOperationKey(request.clientOperationId());
        String requestHash = operationKey == null
                ? null
                : RequestFingerprint.sha256(turnoId, request);
        if (operationKey != null) {
            CajaMovimiento completed = cajaMovimientoRepository.findByClientOperationId(operationKey)
                    .orElse(null);
            if (completed != null) {
                if (!completed.getRequestHash().equals(requestHash)) {
                    throw new BusinessException(
                            "OPERACION_CAJA_REUTILIZADA",
                            "El identificador de operacion ya fue usado con datos diferentes"
                    );
                }
                return CajaMapper.toMovimientoResponse(completed);
            }
        }

        CajaMovimiento movimiento = cajaMovimientoService.registrar(
                turno,
                request.tipoMovimiento(),
                "MANUAL",
                "EFECTIVO",
                request.monto(),
                request.descripcion(),
                request.referencia(),
                null,
                null,
                operationKey,
                requestHash
        );
        cajaTurnoRepository.save(turno);
        return CajaMapper.toMovimientoResponse(movimiento);
    }

    private String normalizeOperationKey(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String operationKey = value.trim();
        if (operationKey.length() > 100) {
            throw new BusinessException(
                    "OPERACION_CAJA_INVALIDA",
                    "El identificador de operacion no puede superar 100 caracteres"
            );
        }
        return operationKey;
    }
}
