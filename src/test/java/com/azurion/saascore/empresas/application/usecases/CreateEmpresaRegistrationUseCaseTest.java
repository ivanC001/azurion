package com.azurion.saascore.empresas.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.saascore.empresas.application.dto.CreateEmpresaRegistrationRequest;
import com.azurion.saascore.empresas.application.dto.CreateEmpresaRequest;
import com.azurion.saascore.empresas.application.dto.EmpresaResponse;
import com.azurion.saascore.planes.domain.entities.Plan;
import com.azurion.saascore.planes.domain.repositories.PlanModuloRepository;
import com.azurion.saascore.planes.domain.repositories.PlanRepository;
import com.azurion.saascore.suscripciones.application.dto.CreateSuscripcionRequest;
import com.azurion.saascore.suscripciones.application.dto.SuscripcionResponse;
import com.azurion.saascore.suscripciones.application.usecases.CreateSuscripcionUseCase;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CreateEmpresaRegistrationUseCaseTest {

    @Test
    void modulesAreAlwaysDerivedFromSelectedPlan() {
        PlanRepository planRepository = mock(PlanRepository.class);
        PlanModuloRepository planModuloRepository = mock(PlanModuloRepository.class);
        CreateEmpresaUseCase createEmpresaUseCase = mock(CreateEmpresaUseCase.class);
        CreateSuscripcionUseCase createSuscripcionUseCase = mock(CreateSuscripcionUseCase.class);
        Plan plan = new Plan();
        plan.setId(7L);
        plan.setEstado("ACTIVO");
        when(planRepository.findById(7L)).thenReturn(Optional.of(plan));
        when(planModuloRepository.findModuloCodigosByPlanId(7L)).thenReturn(List.of("CRM", "ERP"));
        EmpresaResponse empresa = mock(EmpresaResponse.class);
        when(empresa.id()).thenReturn(44L);
        when(createEmpresaUseCase.execute(org.mockito.ArgumentMatchers.any())).thenReturn(empresa);
        when(createSuscripcionUseCase.execute(org.mockito.ArgumentMatchers.any()))
                .thenReturn(mock(SuscripcionResponse.class));
        CreateEmpresaRegistrationUseCase useCase = new CreateEmpresaRegistrationUseCase(
                planRepository,
                planModuloRepository,
                createEmpresaUseCase,
                createSuscripcionUseCase
        );

        useCase.execute(new CreateEmpresaRegistrationRequest(
                "20601234567",
                "Empresa Segura SAC",
                "RUC",
                null,
                "PE",
                "Peru",
                "PEN",
                "S/",
                "America/Lima",
                "es-PE",
                "empresa_segura",
                "tenant_empresa_segura",
                7L,
                LocalDate.of(2026, 7, 31),
                null
        ));

        ArgumentCaptor<CreateEmpresaRequest> empresaRequest = ArgumentCaptor.forClass(CreateEmpresaRequest.class);
        verify(createEmpresaUseCase).execute(empresaRequest.capture());
        assertThat(empresaRequest.getValue().moduloCodigos()).containsExactly("CRM", "ERP");

        ArgumentCaptor<CreateSuscripcionRequest> subscriptionRequest =
                ArgumentCaptor.forClass(CreateSuscripcionRequest.class);
        verify(createSuscripcionUseCase).execute(subscriptionRequest.capture());
        assertThat(subscriptionRequest.getValue().empresaId()).isEqualTo(44L);
        assertThat(subscriptionRequest.getValue().planId()).isEqualTo(7L);
    }
}
