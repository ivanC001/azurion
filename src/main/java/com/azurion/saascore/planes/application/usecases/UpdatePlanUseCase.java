package com.azurion.saascore.planes.application.usecases;

import com.azurion.saascore.planes.application.dto.PlanResponse;
import com.azurion.saascore.planes.application.dto.UpdatePlanRequest;
import com.azurion.saascore.planes.application.mappers.PlanMapper;
import com.azurion.saascore.modulos.application.services.PlatformModuleAuditService;
import com.azurion.saascore.planes.domain.entities.Plan;
import com.azurion.saascore.planes.domain.repositories.PlanRepository;
import com.azurion.shared.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdatePlanUseCase {

    private final PlanRepository planRepository;
    private final AsignarModulosPlanUseCase asignarModulosPlanUseCase;
    private final PlatformModuleAuditService auditService;

    @Transactional
    public PlanResponse execute(Long id, UpdatePlanRequest request) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new BusinessException("PLAN_NOT_FOUND", "Plan not found: " + id));

        plan.setNombre(request.nombre());
        plan.setDescripcion(request.descripcion());
        plan.setLimiteMensualBolsa(request.limiteMensualBolsa());
        plan.setPrecioMensual(request.precioMensual());
        plan.setEstado(request.estado().trim().toUpperCase());

        Plan saved = planRepository.save(plan);
        var moduloCodigos = request.moduloCodigos() == null
                ? asignarModulosPlanUseCase.execute(saved.getId(), List.of())
                : asignarModulosPlanUseCase.execute(saved.getId(), request.moduloCodigos());

        auditService.record("/plans/" + id, "Actualizacion de plan " + saved.getCodigo());
        return PlanMapper.toResponse(saved, moduloCodigos);
    }
}
