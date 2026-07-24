package com.azurion.saascore.suscripciones.application.usecases;

import com.azurion.saascore.configuracion.application.dto.EmpresaModuloAssignmentRequest;
import com.azurion.saascore.configuracion.application.dto.SyncEmpresaModulosRequest;
import com.azurion.saascore.configuracion.application.usecases.AsignarModulosEmpresaUseCase;
import com.azurion.saascore.configuracion.domain.entities.EmpresaModulo;
import com.azurion.saascore.configuracion.domain.repositories.EmpresaModuloRepository;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.saascore.modulos.domain.entities.Modulo;
import com.azurion.saascore.modulos.domain.repositories.ModuloRepository;
import com.azurion.saascore.planes.domain.entities.Plan;
import com.azurion.saascore.planes.domain.repositories.PlanModuloRepository;
import com.azurion.saascore.planes.domain.repositories.PlanRepository;
import com.azurion.saascore.suscripciones.application.dto.SuscripcionResponse;
import com.azurion.saascore.suscripciones.application.dto.UpdateEmpresaSubscriptionPlanRequest;
import com.azurion.saascore.suscripciones.application.mappers.SuscripcionMapper;
import com.azurion.saascore.suscripciones.domain.entities.Suscripcion;
import com.azurion.saascore.suscripciones.domain.repositories.SuscripcionRepository;
import com.azurion.saascore.usuarios.application.services.EmpresaTenantUserCountService;
import com.azurion.shared.exception.BusinessException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateEmpresaSubscriptionPlanUseCase {

    private final EmpresaRepository empresaRepository;
    private final PlanRepository planRepository;
    private final PlanModuloRepository planModuloRepository;
    private final ModuloRepository moduloRepository;
    private final EmpresaModuloRepository empresaModuloRepository;
    private final SuscripcionRepository suscripcionRepository;
    private final EmpresaTenantUserCountService userCountService;
    private final AsignarModulosEmpresaUseCase asignarModulosEmpresaUseCase;

    @Transactional
    public SuscripcionResponse execute(
            Long empresaId,
            UpdateEmpresaSubscriptionPlanRequest request
    ) {
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new BusinessException(
                        "EMPRESA_NOT_FOUND",
                        "No se encontro la empresa seleccionada"
                ));
        Plan plan = planRepository.findById(request.planId())
                .orElseThrow(() -> new BusinessException(
                        "PLAN_NOT_FOUND",
                        "No se encontro el plan seleccionado"
                ));
        if (!"ACTIVO".equalsIgnoreCase(plan.getEstado())) {
            throw new BusinessException("PLAN_NO_DISPONIBLE", "El plan seleccionado no esta activo");
        }

        List<Suscripcion> activeSubscriptions =
                suscripcionRepository.findAllActiveStateForUpdate(empresaId);
        int effectiveLimit = request.limiteUsuarios() == null
                ? plan.getLimiteUsuarios()
                : request.limiteUsuarios();
        long activeUsers = userCountService.countActiveUsers(empresa);
        if (effectiveLimit < activeUsers) {
            throw new BusinessException(
                    "CUPO_MENOR_A_USUARIOS_ACTIVOS",
                    "La empresa tiene " + activeUsers
                            + " usuario(s) activos; el cupo no puede ser menor"
            );
        }

        Suscripcion subscription = activeSubscriptions.stream()
                .findFirst()
                .orElseGet(Suscripcion::new);
        boolean planChanged = subscription.getId() == null
                || subscription.getPlan() == null
                || !plan.getId().equals(subscription.getPlan().getId());

        LocalDate today = LocalDate.now();
        activeSubscriptions.stream().skip(1).forEach(duplicate -> {
            duplicate.setEstado("SUSPENDIDA");
            duplicate.setFechaFin(today);
            suscripcionRepository.save(duplicate);
        });

        if (subscription.getId() == null) {
            subscription.setEmpresa(empresa);
            subscription.setEstado("ACTIVA");
            subscription.setFechaInicio(today);
        }
        subscription.setPlan(plan);
        subscription.setLimiteUsuarios(request.limiteUsuarios());
        subscription.setFechaFin(null);
        Suscripcion saved = suscripcionRepository.save(subscription);

        List<String> planModuleCodes =
                planModuloRepository.findModuloCodigosByPlanId(plan.getId());
        if (planChanged && planModuleCodes.isEmpty()) {
            throw new BusinessException(
                    "PLAN_SIN_MODULOS",
                    "Configura al menos un modulo en el plan antes de asignarlo a una empresa"
            );
        }
        if (!planModuleCodes.isEmpty()) {
            synchronizeCompanyModules(empresa, planModuleCodes, today);
        }

        return SuscripcionMapper.toResponse(saved);
    }

    private void synchronizeCompanyModules(
            Empresa empresa,
            List<String> planModuleCodes,
            LocalDate today
    ) {
        Set<String> includedCodes = planModuleCodes.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
        Map<Long, EmpresaModulo> currentByModuleId = empresaModuloRepository
                .findDetailedByEmpresaId(empresa.getId())
                .stream()
                .collect(Collectors.toMap(
                        item -> item.getModulo().getId(),
                        Function.identity()
                ));

        List<EmpresaModuloAssignmentRequest> assignments = moduloRepository
                .findAllByOrderByNombreAsc()
                .stream()
                .map(module -> assignment(
                        module,
                        currentByModuleId.get(module.getId()),
                        includedCodes.contains(module.getCodigo().toUpperCase()),
                        today
                ))
                .toList();
        asignarModulosEmpresaUseCase.execute(
                empresa.getId(),
                new SyncEmpresaModulosRequest(assignments)
        );
    }

    private EmpresaModuloAssignmentRequest assignment(
            Modulo module,
            EmpresaModulo current,
            boolean active,
            LocalDate today
    ) {
        return new EmpresaModuloAssignmentRequest(
                module.getId(),
                module.getCodigo(),
                active ? "ACTIVO" : "INACTIVO",
                active,
                current == null || current.getFechaInicio() == null
                        ? today
                        : current.getFechaInicio(),
                active ? null : today,
                current == null ? null : current.getConfiguracionExtra()
        );
    }
}
