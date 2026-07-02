package com.azurion.saascore.planes.application.usecases;

import com.azurion.saascore.modulos.domain.entities.Modulo;
import com.azurion.saascore.modulos.domain.repositories.ModuloRepository;
import com.azurion.saascore.modulos.application.services.PlatformModuleAuditService;
import com.azurion.saascore.planes.domain.entities.Plan;
import com.azurion.saascore.planes.domain.entities.PlanModulo;
import com.azurion.saascore.planes.domain.repositories.PlanModuloRepository;
import com.azurion.saascore.planes.domain.repositories.PlanRepository;
import com.azurion.shared.exception.BusinessException;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AsignarModulosPlanUseCase {

    private final PlanRepository planRepository;
    private final ModuloRepository moduloRepository;
    private final PlanModuloRepository planModuloRepository;
    private final PlatformModuleAuditService auditService;

    @Transactional
    public List<String> execute(Long planId, List<String> moduloCodigos) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException("PLAN_NOT_FOUND", "Plan not found: " + planId));

        LinkedHashSet<String> normalizedCodes = new LinkedHashSet<>();
        if (moduloCodigos != null) {
            moduloCodigos.stream()
                    .filter(code -> code != null && !code.isBlank())
                    .map(code -> code.trim().toUpperCase())
                    .forEach(normalizedCodes::add);
        }

        planModuloRepository.deleteByPlanId(plan.getId());

        for (String moduleCode : normalizedCodes) {
            Modulo modulo = moduloRepository.findByCodigoIgnoreCase(moduleCode)
                    .orElseThrow(() -> new BusinessException("MODULO_NOT_FOUND", "Modulo not found: " + moduleCode));

            PlanModulo planModulo = new PlanModulo();
            planModulo.setPlan(plan);
            planModulo.setModulo(modulo);
            planModuloRepository.save(planModulo);
        }

        auditService.record(
                "/plans/" + plan.getId() + "/modules",
                "Cambio de modulos del plan " + plan.getCodigo() + ": " + normalizedCodes
        );

        return List.copyOf(normalizedCodes);
    }
}
