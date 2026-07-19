package com.azurion.saascore.usuarios.application.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.modulos.application.services.ModuleAccessService;
import com.azurion.saascore.roles.domain.entities.Rol;
import com.azurion.saascore.roles.domain.entities.RoleScope;
import com.azurion.saascore.roles.domain.repositories.RolRepository;
import com.azurion.shared.exception.BusinessException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class TenantRoleAssignmentAuthorizerTest {

    @Mock
    private RolRepository rolRepository;

    @Mock
    private ModuleAccessService moduleAccessService;

    private TenantRoleAssignmentAuthorizer authorizer;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        authorizer = new TenantRoleAssignmentAuthorizer(rolRepository, moduleAccessService);
        TenantContext.setTenantId("tenant_pruebas");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin-empresa",
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN_EMPRESA"))
        ));
    }

    @AfterEach
    void tearDown() throws Exception {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
        mocks.close();
    }

    @ParameterizedTest(name = "{0} requiere modulo {2}")
    @CsvSource({
            "ERP_ADMIN,ERP,ERP",
            "ERP_VENDEDOR,ERP,ERP",
            "ERP_CAJERO,ERP,ERP",
            "ERP_ALMACENERO,ERP,ERP",
            "ERP_CONTADOR,ERP,ERP",
            "CRM_ADMIN,CRM,CRM",
            "CRM_GERENTE,CRM,CRM",
            "CRM_SUPERVISOR,CRM,CRM",
            "CRM_VENDEDOR,CRM,CRM",
            "CRM_MARKETING,CRM,CRM",
            "CRM_CALLCENTER,CRM,CRM"
    })
    void shouldAssignPureRoleWhenProductModuleIsActive(String code, RoleScope scope, String module) {
        when(rolRepository.findByCodigoIgnoreCase(code)).thenReturn(Optional.of(role(code, scope, false)));
        when(moduleAccessService.hasCurrentTenantModule(module)).thenReturn(true);

        assertDoesNotThrow(() -> authorizer.assertCanAssign("tenant_pruebas", code));
    }

    @ParameterizedTest(name = "{0} se bloquea sin modulo {2}")
    @CsvSource({
            "ERP_ADMIN,ERP,ERP",
            "ERP_VENDEDOR,ERP,ERP",
            "ERP_CAJERO,ERP,ERP",
            "ERP_ALMACENERO,ERP,ERP",
            "ERP_CONTADOR,ERP,ERP",
            "CRM_ADMIN,CRM,CRM",
            "CRM_GERENTE,CRM,CRM",
            "CRM_SUPERVISOR,CRM,CRM",
            "CRM_VENDEDOR,CRM,CRM",
            "CRM_MARKETING,CRM,CRM",
            "CRM_CALLCENTER,CRM,CRM"
    })
    void shouldRejectPureRoleWhenProductModuleIsDisabled(String code, RoleScope scope, String module) {
        when(rolRepository.findByCodigoIgnoreCase(code)).thenReturn(Optional.of(role(code, scope, false)));
        when(moduleAccessService.hasCurrentTenantModule(module)).thenReturn(false);

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> authorizer.assertCanAssign("tenant_pruebas", code)
        );

        assertEquals("MODULO_NO_ACTIVO", error.getCode());
    }

    @ParameterizedTest
    @CsvSource({"VENDEDOR", "SUPERVISOR_SUCURSAL", "AUDITOR"})
    void shouldRejectDeprecatedHybridRoles(String code) {
        when(rolRepository.findByCodigoIgnoreCase(code)).thenReturn(Optional.of(role(code, RoleScope.MIXED, true)));

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> authorizer.assertCanAssign("tenant_pruebas", code)
        );

        assertEquals("ROL_DEPRECADO", error.getCode());
    }

    private Rol role(String code, RoleScope scope, boolean deprecated) {
        Rol role = new Rol();
        role.setCodigo(code);
        role.setAmbito(scope);
        role.setDeprecated(deprecated);
        role.setActivo(true);
        return role;
    }
}
