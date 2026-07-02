package com.azurion.saascore.caja.application.usecases;

import com.azurion.saascore.caja.application.dto.CajaResponse;
import com.azurion.saascore.auth.application.services.AuthorizationService;
import com.azurion.saascore.caja.application.dto.CerrarCajaRequest;
import com.azurion.saascore.caja.application.mappers.CajaMapper;
import com.azurion.saascore.caja.domain.entities.Caja;
import com.azurion.saascore.caja.domain.repositories.CajaRepository;
import com.azurion.shared.exception.BusinessException;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CerrarCajaUseCase {

    private static final String ABIERTA = "ABIERTA";
    private static final String CERRADA = "CERRADA";

    private final CajaRepository cajaRepository;
    private final AuthorizationService authorizationService;

    @Transactional
    public CajaResponse execute(Long cajaId, CerrarCajaRequest request) {
        authorizationService.validarCaja(authorizationService.currentUsuarioId(), cajaId);
        Caja caja = cajaRepository.findById(cajaId)
                .orElseThrow(() -> new BusinessException("CAJA_NO_ENCONTRADA", "Caja no encontrada"));

        if (!ABIERTA.equals(caja.getEstado())) {
            throw new BusinessException("CAJA_NO_ABIERTA", "Solo se puede cerrar una caja abierta");
        }

        caja.setEstado(CERRADA);
        caja.setSaldoSalida(request.saldoSalida());
        caja.setDiferenciaCierre(request.saldoSalida().subtract(caja.getSaldoActual()));
        caja.setResponsableCierreId(request.responsableId());
        caja.setResponsableCierreNombre(request.responsableNombre());
        caja.setFechaCierre(OffsetDateTime.now());
        caja.setObservacionCierre(request.observacion());

        return CajaMapper.toResponse(cajaRepository.save(caja));
    }
}
