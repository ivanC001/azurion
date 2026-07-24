package com.azurion.saascore.empresas.application.usecases;

import com.azurion.saascore.configuracion.domain.repositories.EmpresaModuloRepository;
import com.azurion.saascore.empresas.application.dto.EmpresaOperationalSummaryResponse;
import com.azurion.saascore.empresas.application.mappers.EmpresaMapper;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.saascore.suscripciones.application.mappers.SuscripcionMapper;
import com.azurion.saascore.suscripciones.domain.entities.Suscripcion;
import com.azurion.saascore.suscripciones.domain.repositories.SuscripcionRepository;
import com.azurion.saascore.usuarios.application.services.EmpresaTenantUserCountService;
import com.azurion.saascore.usuarios.application.services.EmpresaTenantUserCountService.TenantUserCounts;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListEmpresaOperationalSummariesUseCase {

    private final EmpresaRepository empresaRepository;
    private final SuscripcionRepository suscripcionRepository;
    private final EmpresaModuloRepository empresaModuloRepository;
    private final EmpresaTenantUserCountService userCountService;

    @Transactional(readOnly = true)
    public List<EmpresaOperationalSummaryResponse> execute() {
        LocalDate today = LocalDate.now();
        Map<Long, Suscripcion> subscriptionByCompany = currentSubscriptions(today);

        return empresaRepository.findAllByOrderByRazonSocialAsc().stream()
                .map(empresa -> toSummary(
                        empresa,
                        subscriptionByCompany.get(empresa.getId()),
                        today
                ))
                .toList();
    }

    private Map<Long, Suscripcion> currentSubscriptions(LocalDate today) {
        Map<Long, Suscripcion> current = new HashMap<>();
        for (Suscripcion subscription : suscripcionRepository.findAllByOrderByIdDesc()) {
            Long empresaId = subscription.getEmpresa().getId();
            Suscripcion selected = current.get(empresaId);
            if (selected == null || isCurrent(subscription, today) && !isCurrent(selected, today)) {
                current.put(empresaId, subscription);
            }
        }
        return current;
    }

    private boolean isCurrent(Suscripcion subscription, LocalDate today) {
        return "ACTIVA".equalsIgnoreCase(subscription.getEstado())
                && !subscription.getFechaInicio().isAfter(today)
                && (subscription.getFechaFin() == null || !subscription.getFechaFin().isBefore(today));
    }

    private EmpresaOperationalSummaryResponse toSummary(
            Empresa empresa,
            Suscripcion subscription,
            LocalDate today
    ) {
        List<String> modules = empresaModuloRepository.findActiveModuleCodes(empresa.getId(), today);
        TenantUserCounts userCounts = safeUserCounts(empresa);
        Integer effectiveLimit = subscription == null
                ? null
                : subscription.getLimiteUsuarios() == null
                        ? subscription.getPlan().getLimiteUsuarios()
                        : subscription.getLimiteUsuarios();
        Long activeUsers = userCounts == null ? null : userCounts.active();
        Integer availableSeats = effectiveLimit == null || activeUsers == null
                ? null
                : (int) Math.max((long) effectiveLimit - activeUsers, 0L);
        boolean exceeded = effectiveLimit != null
                && activeUsers != null
                && activeUsers > effectiveLimit;

        return new EmpresaOperationalSummaryResponse(
                EmpresaMapper.toResponse(empresa),
                subscription == null ? null : SuscripcionMapper.toResponse(subscription),
                subscription != null && isCurrent(subscription, today),
                subscription == null ? null : subscription.getPlan().getPrecioMensual(),
                subscription == null ? null : subscription.getPlan().getLimiteMensualBolsa(),
                userCounts == null ? null : userCounts.total(),
                activeUsers,
                userCounts == null ? null : userCounts.inactive(),
                availableSeats,
                exceeded,
                userCounts != null,
                List.copyOf(modules),
                empresa.getCreatedAt(),
                empresa.getUpdatedAt()
        );
    }

    private TenantUserCounts safeUserCounts(Empresa empresa) {
        try {
            return userCountService.countUsers(empresa);
        } catch (RuntimeException exception) {
            log.warn(
                    "No se pudo contar usuarios para empresaId={} tenantId={}: {}",
                    empresa.getId(),
                    empresa.getTenantId(),
                    exception.getMessage()
            );
            return null;
        }
    }
}
