package com.azurion.saascore.crm.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.auth.application.services.AuthorizationService;
import com.azurion.saascore.crm.domain.entities.CrmMeta;
import com.azurion.saascore.crm.domain.repositories.CrmActividadRepository;
import com.azurion.saascore.crm.domain.repositories.CrmCurrencyConfigRepository;
import com.azurion.saascore.crm.domain.repositories.CrmMetaRepository;
import com.azurion.saascore.crm.domain.repositories.CrmOportunidadRepository;
import com.azurion.saascore.crm.domain.repositories.CrmProspectoRepository;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.saascore.usuarios.domain.repositories.UsuarioTenantRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CrmGoalServiceTest {

    @Mock private CrmMetaRepository goalRepository;
    @Mock private CrmOportunidadRepository opportunityRepository;
    @Mock private CrmProspectoRepository prospectRepository;
    @Mock private CrmActividadRepository activityRepository;
    @Mock private CrmCurrencyConfigRepository currencyRepository;
    @Mock private EmpresaRepository companyRepository;
    @Mock private UsuarioTenantRepository userRepository;
    @Mock private AuthorizationService authorizationService;

    private CrmGoalService service;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("tenant-demo");
        when(authorizationService.currentUsuarioId()).thenReturn(7L);
        service = new CrmGoalService(
                goalRepository,
                opportunityRepository,
                prospectRepository,
                activityRepository,
                currencyRepository,
                companyRepository,
                userRepository,
                authorizationService
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void calculatesMonthlyTeamProgressFromRealOperations() {
        authenticate("CRM_GOALS_MANAGE");
        Empresa company = new Empresa();
        company.setMonedaCodigo("PEN");
        when(companyRepository.findByTenantId("tenant-demo")).thenReturn(Optional.of(company));
        when(goalRepository.findByAnioAndMesOrderByAlcanceAscResponsableIdAsc(2026, 8))
                .thenReturn(List.of(goal(1L, "EQUIPO", null)));
        when(opportunityRepository.summarizeGoalsByOwner(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(List.of(opportunityAggregate("7", "PEN", 2, 4, "1000")));
        when(prospectRepository.countGoalsByOwner(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(ownerCount("7", 3)));
        when(activityRepository.countGoalsByOwner(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(List.of(activityCount("7", 8)));

        var result = service.list(2026, 8).getFirst();

        assertEquals(new BigDecimal("1000.00"), result.actualIngresos());
        assertEquals(2, result.actualOportunidadesGanadas());
        assertEquals(3, result.actualProspectosNuevos());
        assertEquals(8, result.actualActividadesRealizadas());
        assertEquals(new BigDecimal("50.00"), result.actualConversion());
        assertEquals(50, result.progresoIngresos());
        assertEquals(100, result.progresoOportunidadesGanadas());
        assertEquals(60, result.progresoProspectosNuevos());
        assertEquals(80, result.progresoActividadesRealizadas());
        assertEquals(100, result.progresoConversion());
    }

    @Test
    void sellerSeesOnlyTeamAndOwnGoals() {
        authenticate("CRM_GOALS_READ");
        when(goalRepository.findByAnioAndMesOrderByAlcanceAscResponsableIdAsc(2026, 8))
                .thenReturn(List.of(
                        goal(1L, "EQUIPO", null),
                        goal(2L, "ASESOR", "7"),
                        goal(3L, "ASESOR", "8")
                ));
        when(opportunityRepository.summarizeGoalsByOwner(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(List.of());
        when(prospectRepository.countGoalsByOwner(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(activityRepository.countGoalsByOwner(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(List.of());
        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        var result = service.list(2026, 8);

        assertEquals(2, result.size());
        assertEquals(List.of("EQUIPO", "ASESOR"), result.stream().map(item -> item.alcance()).toList());
        assertEquals(
                java.util.Arrays.asList(null, "7"),
                result.stream().map(item -> item.responsableId()).toList()
        );
    }

    private void authenticate(String authority) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "seller.demo",
                "n/a",
                List.of(new SimpleGrantedAuthority(authority))
        ));
    }

    private CrmMeta goal(Long id, String scope, String owner) {
        CrmMeta goal = new CrmMeta();
        goal.setId(id);
        goal.setAnio(2026);
        goal.setMes(8);
        goal.setAlcance(scope);
        goal.setResponsableId(owner);
        goal.setMoneda("PEN");
        goal.setMetaIngresos(new BigDecimal("2000"));
        goal.setMetaOportunidadesGanadas(2);
        goal.setMetaProspectosNuevos(5);
        goal.setMetaActividadesRealizadas(10);
        goal.setMetaConversion(new BigDecimal("50"));
        goal.setCreatedBy("7");
        return goal;
    }

    private CrmOportunidadRepository.GoalAggregateProjection opportunityAggregate(
            String owner,
            String currency,
            long won,
            long closed,
            String amount
    ) {
        return new CrmOportunidadRepository.GoalAggregateProjection() {
            public String getResponsableId() { return owner; }
            public String getMoneda() { return currency; }
            public long getGanadas() { return won; }
            public long getCerradas() { return closed; }
            public BigDecimal getMontoGanado() { return new BigDecimal(amount); }
        };
    }

    private CrmProspectoRepository.OwnerCountProjection ownerCount(String owner, long count) {
        return new CrmProspectoRepository.OwnerCountProjection() {
            public String getResponsableId() { return owner; }
            public long getCantidad() { return count; }
        };
    }

    private CrmActividadRepository.OwnerCountProjection activityCount(String owner, long count) {
        return new CrmActividadRepository.OwnerCountProjection() {
            public String getResponsableId() { return owner; }
            public long getCantidad() { return count; }
        };
    }
}
