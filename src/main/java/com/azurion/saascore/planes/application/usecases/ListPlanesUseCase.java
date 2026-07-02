package com.azurion.saascore.planes.application.usecases;

import com.azurion.saascore.planes.application.dto.PlanResponse;
import com.azurion.saascore.planes.application.mappers.PlanMapper;
import com.azurion.saascore.planes.domain.repositories.PlanModuloRepository;
import com.azurion.saascore.planes.domain.repositories.PlanRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListPlanesUseCase {

    private final PlanRepository planRepository;
    private final PlanModuloRepository planModuloRepository;

    public List<PlanResponse> execute() {
        return planRepository.findAll().stream()
                .map(plan -> PlanMapper.toResponse(plan, planModuloRepository.findModuloCodigosByPlanId(plan.getId())))
                .toList();
    }
}
