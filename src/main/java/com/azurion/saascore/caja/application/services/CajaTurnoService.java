package com.azurion.saascore.caja.application.services;

import com.azurion.saascore.auth.application.services.AuthorizationService;
import com.azurion.saascore.caja.application.dto.AbrirCajaTurnoRequest;
import com.azurion.saascore.caja.application.dto.CajaTurnoResponse;
import com.azurion.saascore.caja.application.dto.CerrarCajaTurnoRequest;
import com.azurion.saascore.caja.application.mappers.CajaMapper;
import com.azurion.saascore.caja.domain.entities.CajaFisica;
import com.azurion.saascore.caja.domain.entities.CajaTurno;
import com.azurion.saascore.caja.domain.repositories.CajaFisicaRepository;
import com.azurion.saascore.caja.domain.repositories.CajaTurnoRepository;
import com.azurion.saascore.sucursales.application.services.SucursalOperationalGuard;
import com.azurion.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CajaTurnoService {

    public static final String ABIERTO = "ABIERTO";
    public static final String CERRADO = "CERRADO";
    private static final int LEGACY_LIST_LIMIT = 200;

    private final CajaFisicaRepository cajaFisicaRepository;
    private final CajaTurnoRepository cajaTurnoRepository;
    private final AuthorizationService authorizationService;
    private final CajaActorService cajaActorService;
    private final SucursalOperationalGuard sucursalOperationalGuard;

    @Transactional(readOnly = true)
    public List<CajaTurnoResponse> list(String estado, Long sucursalId) {
        String normalizedState = normalizeState(estado);
        Long usuarioId = authorizationService.currentUsuarioId();
        boolean viewOthers = authorizationService.esAdministradorActual()
                || authorizationService.tieneAutoridad("CAJA_VIEW_OTHERS");
        PageRequest limit = PageRequest.of(0, LEGACY_LIST_LIMIT);
        List<CajaTurno> turnos;
        if (!viewOthers) {
            if (usuarioId == null) {
                turnos = List.of();
            } else if (sucursalId != null && normalizedState != null) {
                turnos = cajaTurnoRepository.findByUsuarioIdAndCajaSucursalIdAndEstadoOrderByFechaAperturaDesc(
                        usuarioId, sucursalId, normalizedState, limit
                );
            } else if (sucursalId != null) {
                turnos = cajaTurnoRepository.findByUsuarioIdAndCajaSucursalIdOrderByFechaAperturaDesc(
                        usuarioId, sucursalId, limit
                );
            } else if (normalizedState != null) {
                turnos = cajaTurnoRepository.findByUsuarioIdAndEstadoOrderByFechaAperturaDesc(
                        usuarioId, normalizedState, limit
                );
            } else {
                turnos = cajaTurnoRepository.findByUsuarioIdOrderByFechaAperturaDesc(usuarioId, limit);
            }
        } else if (sucursalId == null && normalizedState == null) {
            turnos = cajaTurnoRepository.findAllByOrderByFechaAperturaDesc(limit);
        } else if (sucursalId == null) {
            turnos = cajaTurnoRepository.findByEstadoOrderByFechaAperturaDesc(normalizedState, limit);
        } else if (normalizedState == null) {
            turnos = cajaTurnoRepository.findByCajaSucursalIdOrderByFechaAperturaDesc(sucursalId, limit);
        } else {
            turnos = cajaTurnoRepository.findByCajaSucursalIdAndEstadoOrderByFechaAperturaDesc(
                    sucursalId, normalizedState, limit
            );
        }
        return turnos.stream().map(CajaMapper::toTurnoResponse).toList();
    }

    @Transactional(readOnly = true)
    public Optional<CajaTurnoResponse> activeForCurrentUser() {
        Long usuarioId = authorizationService.currentUsuarioId();
        if (usuarioId == null) {
            return Optional.empty();
        }
        return cajaTurnoRepository
                .findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(usuarioId, ABIERTO)
                .map(CajaMapper::toTurnoResponse);
    }

    @Transactional(readOnly = true)
    public CajaTurnoResponse get(Long id) {
        CajaTurno turno = find(id);
        requireAccess(turno, false);
        return CajaMapper.toTurnoResponse(turno);
    }

    @Transactional
    public CajaTurnoResponse open(AbrirCajaTurnoRequest request) {
        CajaActorService.Actor actor = cajaActorService.actual();
        if (actor.usuarioId() == null && !authorizationService.esAdministradorActual()) {
            throw new BusinessException("CAJA_USUARIO_NO_IDENTIFICADO", "No se pudo identificar al cajero");
        }
        authorizationService.validarCaja(actor.usuarioId(), request.cajaId());
        CajaFisica caja = cajaFisicaRepository.findById(request.cajaId())
                .orElseThrow(() -> new BusinessException("CAJA_FISICA_NO_ENCONTRADA", "Caja fisica no encontrada"));
        sucursalOperationalGuard.requireActive(caja.getSucursal());
        if (!"ACTIVA".equals(caja.getEstado())) {
            throw new BusinessException("CAJA_FISICA_INACTIVA", "La caja seleccionada esta inactiva");
        }
        cajaTurnoRepository.findFirstByCajaIdAndEstadoOrderByFechaAperturaDesc(caja.getId(), ABIERTO)
                .ifPresent(existing -> {
                    throw new BusinessException(
                            "CAJA_YA_TIENE_TURNO",
                            "La caja ya tiene el turno " + existing.getNumero() + " abierto"
                    );
                });
        if (actor.usuarioId() != null) {
            cajaTurnoRepository.findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(actor.usuarioId(), ABIERTO)
                    .ifPresent(existing -> {
                        throw new BusinessException(
                                "USUARIO_YA_TIENE_TURNO",
                                "Ya tienes el turno " + existing.getNumero() + " abierto"
                        );
                    });
        }

        BigDecimal openingBalance = money(request.saldoApertura());
        CajaTurno turno = new CajaTurno();
        turno.setNumero("TMP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));
        turno.setCaja(caja);
        turno.setUsuarioId(actor.usuarioId());
        turno.setMoneda(caja.getMoneda());
        turno.setEstado(ABIERTO);
        turno.setFechaApertura(OffsetDateTime.now());
        turno.setSaldoApertura(openingBalance);
        turno.setSaldoEsperado(openingBalance);
        turno.setResponsableAperturaId(actor.referenciaId());
        turno.setResponsableAperturaNombre(actor.nombre());
        turno.setObservacionApertura(trim(request.observacion()));
        initializeTotals(turno);
        CajaTurno saved = cajaTurnoRepository.saveAndFlush(turno);
        saved.setNumero("T-" + String.format(Locale.ROOT, "%08d", saved.getId()));
        return CajaMapper.toTurnoResponse(cajaTurnoRepository.save(saved));
    }

    @Transactional
    public CajaTurnoResponse close(Long id, CerrarCajaTurnoRequest request) {
        CajaTurno turno = findForUpdate(id);
        requireAccess(turno, true);
        requireOpen(turno);
        CajaActorService.Actor actor = cajaActorService.actual();
        BigDecimal count = money(request.conteoFisico());
        turno.setConteoFisico(count);
        turno.setDiferenciaCierre(count.subtract(turno.getSaldoEsperado()));
        turno.setResponsableCierreId(actor.referenciaId());
        turno.setResponsableCierreNombre(actor.nombre());
        turno.setFechaCierre(OffsetDateTime.now());
        turno.setObservacionCierre(trim(request.observacion()));
        turno.setEstado(CERRADO);
        return CajaMapper.toTurnoResponse(cajaTurnoRepository.save(turno));
    }

    @Transactional(readOnly = true)
    public CajaTurno find(Long id) {
        return cajaTurnoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("CAJA_TURNO_NO_ENCONTRADO", "Turno de caja no encontrado"));
    }

    @Transactional
    public CajaTurno findForUpdate(Long id) {
        return cajaTurnoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException("CAJA_TURNO_NO_ENCONTRADO", "Turno de caja no encontrado"));
    }

    public void requireOpen(CajaTurno turno) {
        if (!ABIERTO.equals(turno.getEstado())) {
            throw new BusinessException("CAJA_TURNO_CERRADO", "El turno de caja debe estar abierto");
        }
    }

    public void requireAccess(CajaTurno turno, boolean ownerRequired) {
        Long usuarioId = authorizationService.currentUsuarioId();
        boolean privileged = authorizationService.esAdministradorActual()
                || authorizationService.tieneAutoridad("CAJA_VIEW_OTHERS");
        if (!privileged && (usuarioId == null || !usuarioId.equals(turno.getUsuarioId()))) {
            throw new BusinessException("CAJA_TURNO_OTRO_USUARIO", "El turno pertenece a otro cajero");
        }
        authorizationService.validarCaja(usuarioId, turno.getCaja().getId());
    }

    private String normalizeState(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "ABIERTA", "ABIERTO" -> ABIERTO;
            case "CERRADA", "CERRADO" -> CERRADO;
            default -> throw new BusinessException("CAJA_TURNO_ESTADO_INVALIDO", "Use ABIERTO o CERRADO");
        };
    }

    private void initializeTotals(CajaTurno turno) {
        turno.setNumeroVentas(0);
        turno.setTotalVentas(BigDecimal.ZERO);
        turno.setTotalEfectivo(BigDecimal.ZERO);
        turno.setTotalTarjeta(BigDecimal.ZERO);
        turno.setTotalBilleteraDigital(BigDecimal.ZERO);
        turno.setTotalTransferencia(BigDecimal.ZERO);
        turno.setTotalCredito(BigDecimal.ZERO);
        turno.setTotalIngresosManuales(BigDecimal.ZERO);
        turno.setTotalRetiros(BigDecimal.ZERO);
        turno.setTotalDepositos(BigDecimal.ZERO);
        turno.setTotalReembolsos(BigDecimal.ZERO);
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
