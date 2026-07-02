package com.azurion.saascore.planes.application.usecases;

import com.azurion.saascore.planes.application.dto.CreatePlanRequest;
import com.azurion.saascore.planes.application.dto.PlanResponse;
import com.azurion.saascore.planes.application.mappers.PlanMapper;
import com.azurion.saascore.modulos.application.services.PlatformModuleAuditService;
import com.azurion.saascore.planes.domain.entities.Plan;
import com.azurion.saascore.planes.domain.repositories.PlanRepository;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreatePlanUseCase {

    private final PlanRepository planRepository;
    private final AsignarModulosPlanUseCase asignarModulosPlanUseCase;
    private final PlatformModuleAuditService auditService;

    @Transactional
    public PlanResponse execute(CreatePlanRequest request) {
        planRepository.findByCodigoIgnoreCase(request.codigo()).ifPresent(existing -> {
            throw new BusinessException("PLAN_CODE_EXISTS", "A plan with same codigo already exists");
        });

        Plan plan = new Plan();
        plan.setNombre(request.nombre());
        plan.setCodigo(request.codigo().trim().toUpperCase());
        plan.setDescripcion(request.descripcion());
        plan.setLimiteMensualBolsa(request.limiteMensualBolsa());
        plan.setPrecioMensual(request.precioMensual());
        plan.setEstado("ACTIVO");

        Plan saved = planRepository.save(plan);
        var moduloCodigos = asignarModulosPlanUseCase.execute(saved.getId(), request.moduloCodigos());

        auditService.record("/plans", "Creacion de plan " + saved.getCodigo());
        return PlanMapper.toResponse(saved, moduloCodigos);
    }
}
