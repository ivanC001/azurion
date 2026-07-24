package com.azurion.saascore.planes.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.saascore.modulos.application.services.PlatformModuleAuditService;
import com.azurion.saascore.planes.application.dto.UpdatePlanRequest;
import com.azurion.saascore.planes.domain.entities.Plan;
import com.azurion.saascore.planes.domain.repositories.PlanModuloRepository;
import com.azurion.saascore.planes.domain.repositories.PlanRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UpdatePlanUseCaseTest {

    private final PlanRepository planRepository = mock(PlanRepository.class);
    private final PlanModuloRepository planModuloRepository = mock(PlanModuloRepository.class);
    private final AsignarModulosPlanUseCase moduleAssignment =
            mock(AsignarModulosPlanUseCase.class);
    private final PlatformModuleAuditService auditService =
            mock(PlatformModuleAuditService.class);
    private final UpdatePlanUseCase useCase = new UpdatePlanUseCase(
            planRepository,
            planModuloRepository,
            moduleAssignment,
            auditService
    );

    @Test
    void preservesExistingModulesWhenUpdateDoesNotSendThem() {
        Plan plan = new Plan();
        plan.setId(5L);
        plan.setCodigo("BUSINESS");
        when(planRepository.findById(5L)).thenReturn(Optional.of(plan));
        when(planRepository.save(plan)).thenReturn(plan);
        when(planModuloRepository.findModuloCodigosByPlanId(5L))
                .thenReturn(List.of("CRM", "VENTAS"));

        var response = useCase.execute(
                5L,
                new UpdatePlanRequest(
                        "Business",
                        "Plan comercial",
                        1000L,
                        12,
                        new BigDecimal("199.00"),
                        "ACTIVO",
                        null
                )
        );

        assertThat(response.moduloCodigos()).containsExactly("CRM", "VENTAS");
        verify(moduleAssignment, never()).execute(5L, List.of());
    }
}
