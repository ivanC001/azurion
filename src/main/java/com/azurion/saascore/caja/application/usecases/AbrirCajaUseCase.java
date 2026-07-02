package com.azurion.saascore.caja.application.usecases;

import com.azurion.saascore.caja.application.dto.AbrirCajaRequest;
import com.azurion.saascore.auth.application.services.AuthorizationService;
import com.azurion.saascore.caja.application.dto.CajaResponse;
import com.azurion.saascore.caja.application.mappers.CajaMapper;
import com.azurion.saascore.caja.domain.entities.Caja;
import com.azurion.saascore.caja.domain.repositories.CajaRepository;
import com.azurion.saascore.sucursales.domain.entities.Sucursal;
import com.azurion.saascore.sucursales.domain.repositories.SucursalRepository;
import com.azurion.saascore.sucursales.application.services.SucursalOperationalGuard;
import com.azurion.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AbrirCajaUseCase {

    private static final String ABIERTA = "ABIERTA";

    private final CajaRepository cajaRepository;
    private final SucursalRepository sucursalRepository;
    private final AuthorizationService authorizationService;
    private final SucursalOperationalGuard sucursalOperationalGuard;

    @Transactional
    public CajaResponse execute(AbrirCajaRequest request) {
        Long usuarioId = authorizationService.currentUsuarioId();
        authorizationService.validarSucursal(usuarioId, request.sucursalId());
        String codigo = request.codigo().trim().toUpperCase();
        Sucursal sucursal = sucursalRepository.findById(request.sucursalId())
                .orElseThrow(() -> new BusinessException("SUCURSAL_NO_ENCONTRADA", "Sucursal no encontrada"));
        sucursalOperationalGuard.requireActive(sucursal);

        cajaRepository.findFirstBySucursalIdAndCodigoIgnoreCaseAndEstado(sucursal.getId(), codigo, ABIERTA).ifPresent(existing -> {
            throw new BusinessException("CAJA_YA_ABIERTA", "Ya existe una caja abierta con codigo " + codigo);
        });

        cajaRepository.findFirstByResponsableAperturaIdAndEstadoOrderByFechaAperturaDesc(request.responsableId(), ABIERTA).ifPresent(existing -> {
            throw new BusinessException("USUARIO_YA_TIENE_CAJA_ABIERTA", "El usuario ya tiene una caja abierta en este turno");
        });

        Caja caja = new Caja();
        caja.setSucursal(sucursal);
        caja.setCodigo(codigo);
        caja.setNombre(request.nombre());
        caja.setEstado(ABIERTA);
        caja.setSaldoCapital(request.saldoCapital());
        caja.setSaldoActual(request.saldoCapital());
        caja.setSaldoSalida(null);
        caja.setTotalEntradas(BigDecimal.ZERO);
        caja.setTotalSalidas(BigDecimal.ZERO);
        caja.setTotalDepositos(BigDecimal.ZERO);
        caja.setDiferenciaCierre(null);
        caja.setResponsableAperturaId(request.responsableId());
        caja.setResponsableAperturaNombre(request.responsableNombre());
        caja.setFechaApertura(OffsetDateTime.now());
        caja.setObservacionApertura(request.observacion());

        Caja saved = cajaRepository.save(caja);
        authorizationService.asignarCaja(usuarioId, saved.getId());
        return CajaMapper.toResponse(saved);
    }
}
