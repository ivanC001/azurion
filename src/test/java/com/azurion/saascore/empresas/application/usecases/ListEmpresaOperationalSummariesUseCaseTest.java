package com.azurion.saascore.empresas.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.azurion.saascore.configuracion.domain.repositories.EmpresaModuloRepository;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.saascore.planes.domain.entities.Plan;
import com.azurion.saascore.suscripciones.domain.entities.Suscripcion;
import com.azurion.saascore.suscripciones.domain.repositories.SuscripcionRepository;
import com.azurion.saascore.usuarios.application.services.EmpresaTenantUserCountService;
import com.azurion.saascore.usuarios.application.services.EmpresaTenantUserCountService.TenantUserCounts;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListEmpresaOperationalSummariesUseCaseTest {

    private final EmpresaRepository empresaRepository = mock(EmpresaRepository.class);
    private final SuscripcionRepository suscripcionRepository =
            mock(SuscripcionRepository.class);
    private final EmpresaModuloRepository empresaModuloRepository =
            mock(EmpresaModuloRepository.class);
    private final EmpresaTenantUserCountService userCountService =
            mock(EmpresaTenantUserCountService.class);
    private final ListEmpresaOperationalSummariesUseCase useCase =
            new ListEmpresaOperationalSummariesUseCase(
                    empresaRepository,
                    suscripcionRepository,
                    empresaModuloRepository,
                    userCountService
            );

    @Test
    void returnsCompanyPlanUsersSeatsAndModulesInOneSummary() {
        Empresa empresa = company();
        Plan plan = plan();
        Suscripcion active = subscription(20L, empresa, plan, "ACTIVA", LocalDate.now().minusDays(2));
        active.setLimiteUsuarios(12);
        Suscripcion cancelled = subscription(
                21L,
                empresa,
                plan,
                "CANCELADA",
                LocalDate.now().minusDays(1)
        );

        when(empresaRepository.findAllByOrderByRazonSocialAsc()).thenReturn(List.of(empresa));
        when(suscripcionRepository.findAllByOrderByIdDesc())
                .thenReturn(List.of(cancelled, active));
        when(empresaModuloRepository.findActiveModuleCodes(10L, LocalDate.now()))
                .thenReturn(List.of("CRM", "VENTAS"));
        when(userCountService.countUsers(empresa)).thenReturn(new TenantUserCounts(9, 7));

        var summaries = useCase.execute();

        assertThat(summaries).hasSize(1);
        var summary = summaries.getFirst();
        assertThat(summary.empresa().razonSocial()).isEqualTo("Empresa Demo");
        assertThat(summary.suscripcion().id()).isEqualTo(20L);
        assertThat(summary.suscripcionVigente()).isTrue();
        assertThat(summary.precioMensual()).isEqualByComparingTo("149.90");
        assertThat(summary.limiteMensualBolsa()).isEqualTo(5_000L);
        assertThat(summary.usuariosTotal()).isEqualTo(9L);
        assertThat(summary.usuariosActivos()).isEqualTo(7L);
        assertThat(summary.usuariosInactivos()).isEqualTo(2L);
        assertThat(summary.cuposDisponibles()).isEqualTo(5);
        assertThat(summary.cupoExcedido()).isFalse();
        assertThat(summary.conteoUsuariosDisponible()).isTrue();
        assertThat(summary.moduloCodigos()).containsExactly("CRM", "VENTAS");
    }

    @Test
    void keepsCompanyVisibleWhenItsTenantUserCountCannotBeRead() {
        Empresa empresa = company();
        when(empresaRepository.findAllByOrderByRazonSocialAsc()).thenReturn(List.of(empresa));
        when(suscripcionRepository.findAllByOrderByIdDesc()).thenReturn(List.of());
        when(empresaModuloRepository.findActiveModuleCodes(10L, LocalDate.now()))
                .thenReturn(List.of());
        when(userCountService.countUsers(empresa)).thenThrow(new IllegalStateException("schema"));

        var summary = useCase.execute().getFirst();

        assertThat(summary.conteoUsuariosDisponible()).isFalse();
        assertThat(summary.usuariosTotal()).isNull();
        assertThat(summary.usuariosActivos()).isNull();
        assertThat(summary.cuposDisponibles()).isNull();
    }

    private Empresa company() {
        Empresa empresa = new Empresa();
        empresa.setId(10L);
        empresa.setRuc("20000000001");
        empresa.setRazonSocial("Empresa Demo");
        empresa.setTenantId("empresa_demo");
        empresa.setSchemaName("tenant_empresa_demo");
        empresa.setCreatedAt(LocalDateTime.now().minusMonths(1));
        empresa.setUpdatedAt(LocalDateTime.now());
        return empresa;
    }

    private Plan plan() {
        Plan plan = new Plan();
        plan.setId(2L);
        plan.setCodigo("CRM");
        plan.setNombre("Plan CRM");
        plan.setLimiteUsuarios(5);
        plan.setLimiteMensualBolsa(5_000L);
        plan.setPrecioMensual(new BigDecimal("149.90"));
        return plan;
    }

    private Suscripcion subscription(
            Long id,
            Empresa empresa,
            Plan plan,
            String status,
            LocalDate start
    ) {
        Suscripcion subscription = new Suscripcion();
        subscription.setId(id);
        subscription.setEmpresa(empresa);
        subscription.setPlan(plan);
        subscription.setEstado(status);
        subscription.setFechaInicio(start);
        return subscription;
    }
}
