package com.azurion.saascore.planes.application.usecases;

import com.azurion.saascore.planes.application.dto.PlanResponse;
import com.azurion.saascore.planes.application.mappers.PlanMapper;
import com.azurion.saascore.planes.domain.repositories.PlanModuloRepository;
import com.azurion.saascore.planes.domain.repositories.PlanRepository;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetPlanByIdUseCase {

    private final PlanRepository planRepository;
    private final PlanModuloRepository planModuloRepository;

    public PlanResponse execute(Long id) {
        return planRepository.findById(id)
                .map(plan -> PlanMapper.toResponse(plan, planModuloRepository.findModuloCodigosByPlanId(plan.getId())))
                .orElseThrow(() -> new BusinessException("PLAN_NOT_FOUND", "Plan not found: " + id));
    }
}
