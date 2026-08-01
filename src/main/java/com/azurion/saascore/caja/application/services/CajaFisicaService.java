package com.azurion.saascore.caja.application.services;

import com.azurion.saascore.auth.application.services.AuthorizationService;
import com.azurion.saascore.caja.application.dto.CajaFisicaResponse;
import com.azurion.saascore.caja.application.dto.GuardarCajaFisicaRequest;
import com.azurion.saascore.caja.application.mappers.CajaMapper;
import com.azurion.saascore.caja.domain.entities.CajaFisica;
import com.azurion.saascore.caja.domain.repositories.CajaFisicaRepository;
import com.azurion.saascore.caja.domain.repositories.CajaTurnoRepository;
import com.azurion.saascore.sucursales.domain.entities.Sucursal;
import com.azurion.saascore.sucursales.domain.repositories.SucursalRepository;
import com.azurion.saascore.sucursales.application.services.SucursalOperationalGuard;
import com.azurion.saascore.usuarios.domain.entities.UsuarioTenant;
import com.azurion.saascore.usuarios.domain.repositories.UsuarioTenantRepository;
import com.azurion.shared.exception.BusinessException;
import jakarta.persistence.EntityManager;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CajaFisicaService {

    private static final Set<String> ESTADOS = Set.of("ACTIVA", "INACTIVA");
    private static final Set<String> MONEDAS = Set.of("PEN", "USD", "EUR");

    private final CajaFisicaRepository cajaFisicaRepository;
    private final CajaTurnoRepository cajaTurnoRepository;
    private final SucursalRepository sucursalRepository;
    private final UsuarioTenantRepository usuarioTenantRepository;
    private final SucursalOperationalGuard sucursalOperationalGuard;
    private final AuthorizationService authorizationService;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<CajaFisicaResponse> list(Long sucursalId) {
        Long usuarioId = authorizationService.currentUsuarioId();
        List<CajaFisica> cajas = authorizationService.esAdministradorActual()
                || authorizationService.tieneAutoridad("CAJA_CONFIGURE")
                ? cajaFisicaRepository.findAllByOrderBySucursalNombreAscNombreAsc()
                : usuarioId == null ? List.of() : cajaFisicaRepository.findPermitidas(usuarioId);
        return cajas.stream()
                .filter(caja -> sucursalId == null || caja.getSucursal().getId().equals(sucursalId))
                .map(caja -> CajaMapper.toFisicaResponse(caja, assignedUserIds(caja.getId())))
                .toList();
    }

    @Transactional
    public CajaFisicaResponse create(GuardarCajaFisicaRequest request) {
        CajaFisica caja = new CajaFisica();
        apply(caja, request, null);
        CajaFisica saved = cajaFisicaRepository.save(caja);
        synchronizeAssignments(saved.getId(), request.usuarioIds());
        return CajaMapper.toFisicaResponse(saved, assignedUserIds(saved.getId()));
    }

    @Transactional
    public CajaFisicaResponse update(Long id, GuardarCajaFisicaRequest request) {
        CajaFisica caja = find(id);
        apply(caja, request, id);
        if ("INACTIVA".equals(caja.getEstado())
                && cajaTurnoRepository.findFirstByCajaIdAndEstadoOrderByFechaAperturaDesc(id, "ABIERTO").isPresent()) {
            throw new BusinessException(
                    "CAJA_CON_TURNO_ABIERTO",
                    "Cierra el turno activo antes de inhabilitar la caja"
            );
        }
        CajaFisica saved = cajaFisicaRepository.save(caja);
        synchronizeAssignments(saved.getId(), request.usuarioIds());
        return CajaMapper.toFisicaResponse(saved, assignedUserIds(saved.getId()));
    }

    @Transactional(readOnly = true)
    public CajaFisica find(Long id) {
        return cajaFisicaRepository.findById(id)
                .orElseThrow(() -> new BusinessException("CAJA_FISICA_NO_ENCONTRADA", "Caja fisica no encontrada"));
    }

    private void apply(CajaFisica caja, GuardarCajaFisicaRequest request, Long currentId) {
        Sucursal sucursal = sucursalRepository.findById(request.sucursalId())
                .orElseThrow(() -> new BusinessException("SUCURSAL_NO_ENCONTRADA", "Sucursal no encontrada"));
        sucursalOperationalGuard.requireActive(sucursal);
        String codigo = request.codigo().trim().toUpperCase(Locale.ROOT);
        cajaFisicaRepository.findBySucursalIdAndCodigoIgnoreCase(sucursal.getId(), codigo)
                .filter(existing -> currentId == null || !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new BusinessException(
                            "CAJA_FISICA_DUPLICADA",
                            "Ya existe una caja con ese codigo en la sucursal"
                    );
                });

        String estado = request.estado().trim().toUpperCase(Locale.ROOT);
        String moneda = request.moneda().trim().toUpperCase(Locale.ROOT);
        if (!ESTADOS.contains(estado)) {
            throw new BusinessException("CAJA_ESTADO_INVALIDO", "Use ACTIVA o INACTIVA");
        }
        if (!MONEDAS.contains(moneda)) {
            throw new BusinessException("CAJA_MONEDA_INVALIDA", "Use PEN, USD o EUR");
        }
        validateUsers(request.usuarioIds());

        caja.setSucursal(sucursal);
        caja.setCodigo(codigo);
        caja.setNombre(request.nombre().trim());
        caja.setMoneda(moneda);
        caja.setEstado(estado);
    }

    private void validateUsers(List<Long> requestedUserIds) {
        Set<Long> ids = normalizedIds(requestedUserIds);
        if (ids.isEmpty()) {
            throw new BusinessException(
                    "CAJA_SIN_CAJEROS",
                    "Asigna al menos un usuario autorizado a la caja"
            );
        }
        List<UsuarioTenant> users = usuarioTenantRepository.findAllById(ids);
        if (users.size() != ids.size() || users.stream().anyMatch(user -> !user.isActivo())) {
            throw new BusinessException(
                    "CAJA_USUARIO_INVALIDO",
                    "Todos los cajeros asignados deben existir y estar activos"
            );
        }
    }

    private void synchronizeAssignments(Long cajaId, List<Long> requestedUserIds) {
        entityManager.createNativeQuery("DELETE FROM usuario_cajas WHERE caja_id = ?")
                .setParameter(1, cajaId)
                .executeUpdate();
        for (Long usuarioId : normalizedIds(requestedUserIds)) {
            entityManager.createNativeQuery("""
                    INSERT INTO usuario_cajas (usuario_id, caja_id)
                    VALUES (?, ?)
                    ON CONFLICT (usuario_id, caja_id) DO NOTHING
                    """)
                    .setParameter(1, usuarioId)
                    .setParameter(2, cajaId)
                    .executeUpdate();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Long> assignedUserIds(Long cajaId) {
        return ((List<Number>) entityManager.createNativeQuery("""
                SELECT usuario_id
                FROM usuario_cajas
                WHERE caja_id = ?
                ORDER BY usuario_id
                """)
                .setParameter(1, cajaId)
                .getResultList()).stream()
                .map(Number::longValue)
                .toList();
    }

    private Set<Long> normalizedIds(List<Long> values) {
        if (values == null) {
            return Set.of();
        }
        LinkedHashSet<Long> result = new LinkedHashSet<>();
        values.stream().filter(value -> value != null && value > 0).forEach(result::add);
        return result;
    }
}
