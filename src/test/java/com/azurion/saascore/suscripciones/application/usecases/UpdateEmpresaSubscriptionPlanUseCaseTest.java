package com.azurion.saascore.suscripciones.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.saascore.configuracion.application.dto.SyncEmpresaModulosRequest;
import com.azurion.saascore.configuracion.application.usecases.AsignarModulosEmpresaUseCase;
import com.azurion.saascore.configuracion.domain.repositories.EmpresaModuloRepository;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.saascore.modulos.domain.entities.Modulo;
import com.azurion.saascore.modulos.domain.repositories.ModuloRepository;
import com.azurion.saascore.planes.domain.entities.Plan;
import com.azurion.saascore.planes.domain.repositories.PlanModuloRepository;
import com.azurion.saascore.planes.domain.repositories.PlanRepository;
import com.azurion.saascore.suscripciones.application.dto.UpdateEmpresaSubscriptionPlanRequest;
import com.azurion.saascore.suscripciones.domain.entities.Suscripcion;
import com.azurion.saascore.suscripciones.domain.repositories.SuscripcionRepository;
import com.azurion.saascore.usuarios.application.services.EmpresaTenantUserCountService;
import com.azurion.shared.exception.BusinessException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateEmpresaSubscriptionPlanUseCaseTest {

    private final EmpresaRepository empresaRepository = mock(EmpresaRepository.class);
    private final PlanRepository planRepository = mock(PlanRepository.class);
    private final PlanModuloRepository planModuloRepository = mock(PlanModuloRepository.class);
    private final ModuloRepository moduloRepository = mock(ModuloRepository.class);
    private final EmpresaModuloRepository empresaModuloRepository =
            mock(EmpresaModuloRepository.class);
    private final SuscripcionRepository suscripcionRepository =
            mock(SuscripcionRepository.class);
    private final EmpresaTenantUserCountService userCountService =
            mock(EmpresaTenantUserCountService.class);
    private final AsignarModulosEmpresaUseCase moduleAssignment =
            mock(AsignarModulosEmpresaUseCase.class);
    private final UpdateEmpresaSubscriptionPlanUseCase useCase =
            new UpdateEmpresaSubscriptionPlanUseCase(
                    empresaRepository,
                    planRepository,
                    planModuloRepository,
                    moduloRepository,
                    empresaModuloRepository,
                    suscripcionRepository,
                    userCountService,
                    moduleAssignment
            );

    private Empresa empresa;

    @BeforeEach
    void setUp() {
        empresa = new Empresa();
        empresa.setId(10L);
        empresa.setSchemaName("tenant_demo");
        when(empresaRepository.findById(10L)).thenReturn(Optional.of(empresa));
    }

    @Test
    void increasesOnlyThisCompanyUserLimit() {
        Plan plan = plan(1L, "BASIC", 5);
        Suscripcion subscription = subscription(20L, plan);
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(userCountService.countActiveUsers(empresa)).thenReturn(4L);
        when(suscripcionRepository.findAllActiveStateForUpdate(10L))
                .thenReturn(List.of(subscription));
        when(suscripcionRepository.save(subscription)).thenReturn(subscription);
        when(planModuloRepository.findModuloCodigosByPlanId(1L)).thenReturn(List.of());

        var response = useCase.execute(
                10L,
                new UpdateEmpresaSubscriptionPlanRequest(1L, 12)
        );

        assertThat(response.limiteUsuarios()).isEqualTo(12);
        assertThat(response.limiteUsuariosPersonalizado()).isTrue();
        assertThat(subscription.getLimiteUsuarios()).isEqualTo(12);
        verify(moduleAssignment, never()).execute(eq(10L), any());
    }

    @Test
    void changesPlanAndSynchronizesItsModules() {
        Plan previous = plan(1L, "BASIC", 5);
        Plan business = plan(2L, "BUSINESS", 15);
        Suscripcion subscription = subscription(20L, previous);
        Modulo crm = new Modulo();
        crm.setId(30L);
        crm.setCodigo("CRM");
        crm.setNombre("CRM");

        when(planRepository.findById(2L)).thenReturn(Optional.of(business));
        when(userCountService.countActiveUsers(empresa)).thenReturn(4L);
        when(suscripcionRepository.findAllActiveStateForUpdate(10L))
                .thenReturn(List.of(subscription));
        when(suscripcionRepository.save(subscription)).thenReturn(subscription);
        when(planModuloRepository.findModuloCodigosByPlanId(2L))
                .thenReturn(List.of("CRM"));
        when(empresaModuloRepository.findDetailedByEmpresaId(10L))
                .thenReturn(List.of());
        when(moduloRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(crm));
        when(moduleAssignment.execute(eq(10L), any(SyncEmpresaModulosRequest.class)))
                .thenReturn(List.of());

        var response = useCase.execute(
                10L,
                new UpdateEmpresaSubscriptionPlanRequest(2L, null)
        );

        assertThat(response.planId()).isEqualTo(2L);
        assertThat(response.limiteUsuarios()).isEqualTo(15);
        assertThat(response.limiteUsuariosPersonalizado()).isFalse();
        verify(moduleAssignment).execute(eq(10L), any(SyncEmpresaModulosRequest.class));
    }

    @Test
    void rejectsQuotaBelowCurrentActiveUsers() {
        Plan plan = plan(1L, "BASIC", 5);
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(userCountService.countActiveUsers(empresa)).thenReturn(6L);

        assertThatThrownBy(() -> useCase.execute(
                10L,
                new UpdateEmpresaSubscriptionPlanRequest(1L, 5)
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("6 usuario");
    }

    @Test
    void rejectsChangingToPlanWithoutModules() {
        Plan previous = plan(1L, "BASIC", 5);
        Plan empty = plan(2L, "EMPTY", 10);
        Suscripcion subscription = subscription(20L, previous);
        when(planRepository.findById(2L)).thenReturn(Optional.of(empty));
        when(userCountService.countActiveUsers(empresa)).thenReturn(1L);
        when(suscripcionRepository.findAllActiveStateForUpdate(10L))
                .thenReturn(List.of(subscription));
        when(suscripcionRepository.save(subscription)).thenReturn(subscription);
        when(planModuloRepository.findModuloCodigosByPlanId(2L)).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.execute(
                10L,
                new UpdateEmpresaSubscriptionPlanRequest(2L, null)
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("al menos un modulo");
    }

    private Plan plan(Long id, String code, int userLimit) {
        Plan plan = new Plan();
        plan.setId(id);
        plan.setCodigo(code);
        plan.setNombre(code);
        plan.setEstado("ACTIVO");
        plan.setLimiteUsuarios(userLimit);
        return plan;
    }

    private Suscripcion subscription(Long id, Plan plan) {
        Suscripcion subscription = new Suscripcion();
        subscription.setId(id);
        subscription.setEmpresa(empresa);
        subscription.setPlan(plan);
        subscription.setEstado("ACTIVA");
        return subscription;
    }
}
