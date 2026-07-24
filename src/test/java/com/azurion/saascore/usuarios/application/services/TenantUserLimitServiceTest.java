package com.azurion.saascore.usuarios.application.services;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.saascore.planes.domain.entities.Plan;
import com.azurion.saascore.suscripciones.domain.entities.Suscripcion;
import com.azurion.saascore.suscripciones.domain.repositories.SuscripcionRepository;
import com.azurion.saascore.usuarios.domain.repositories.UsuarioTenantRepository;
import com.azurion.shared.exception.BusinessException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantUserLimitServiceTest {

    private final EmpresaRepository empresaRepository = mock(EmpresaRepository.class);
    private final SuscripcionRepository suscripcionRepository = mock(SuscripcionRepository.class);
    private final UsuarioTenantRepository usuarioRepository = mock(UsuarioTenantRepository.class);
    private final TenantUserLimitService service = new TenantUserLimitService(
            empresaRepository,
            suscripcionRepository,
            usuarioRepository
    );

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void allowsActivationBelowPlanLimit() {
        prepareLimit(5, 4);

        assertThatNoException().isThrownBy(service::assertCanActivateAnotherUser);
    }

    @Test
    void rejectsActivationWhenPlanLimitWasReached() {
        prepareLimit(5, 5);

        assertThatThrownBy(service::assertCanActivateAnotherUser)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("hasta 5 usuario");
    }

    @Test
    void usesSubscriptionOverrideInsteadOfPlanDefault() {
        prepareLimit(5, 7, 10);

        assertThatNoException().isThrownBy(service::assertCanActivateAnotherUser);
    }

    @Test
    void rejectsActivationWithoutActiveSubscription() {
        TenantContext.setTenantId("tenant-a");
        Empresa empresa = new Empresa();
        empresa.setId(10L);
        when(empresaRepository.findByTenantId("tenant-a")).thenReturn(Optional.of(empresa));
        when(suscripcionRepository.findActiveForUpdate(10L)).thenReturn(List.of());

        assertThatThrownBy(service::assertCanActivateAnotherUser)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("suscripcion activa");
    }

    private void prepareLimit(int limit, long activeUsers) {
        prepareLimit(limit, activeUsers, null);
    }

    private void prepareLimit(int limit, long activeUsers, Integer override) {
        TenantContext.setTenantId("tenant-a");
        Empresa empresa = new Empresa();
        empresa.setId(10L);
        Plan plan = new Plan();
        plan.setLimiteUsuarios(limit);
        Suscripcion subscription = new Suscripcion();
        subscription.setPlan(plan);
        subscription.setLimiteUsuarios(override);

        when(empresaRepository.findByTenantId("tenant-a")).thenReturn(Optional.of(empresa));
        when(suscripcionRepository.findActiveForUpdate(10L)).thenReturn(List.of(subscription));
        when(usuarioRepository.countByActivoTrue()).thenReturn(activeUsers);
    }
}
