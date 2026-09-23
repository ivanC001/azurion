package com.azurion.saascore.usuarios.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.azurion.saascore.roles.domain.entities.Rol;
import com.azurion.saascore.roles.domain.repositories.RolRepository;
import com.azurion.saascore.usuarios.application.dto.CreateUsuarioTenantRequest;
import com.azurion.saascore.usuarios.application.services.TenantRoleAssignmentAuthorizer;
import com.azurion.saascore.usuarios.application.services.TenantUserLimitService;
import com.azurion.saascore.usuarios.application.services.UsuarioSucursalScopeService;
import com.azurion.saascore.usuarios.domain.entities.UsuarioTenant;
import com.azurion.saascore.usuarios.domain.repositories.UsuarioTenantRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class CreateUsuarioTenantUseCaseTest {

    @Mock
    private UsuarioTenantRepository usuarioTenantRepository;
    @Mock
    private RolRepository rolRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TenantRoleAssignmentAuthorizer tenantRoleAssignmentAuthorizer;
    @Mock
    private UsuarioSucursalScopeService usuarioSucursalScopeService;
    @Mock
    private TenantUserLimitService tenantUserLimitService;

    private CreateUsuarioTenantUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateUsuarioTenantUseCase(
                usuarioTenantRepository,
                rolRepository,
                passwordEncoder,
                tenantRoleAssignmentAuthorizer,
                usuarioSucursalScopeService,
                tenantUserLimitService
        );
    }

    @Test
    void guardaNombresYApellidosRecortadosAlRegistrar() {
        stubHappyPath();

        useCase.execute(new CreateUsuarioTenantRequest(
                "vendedor2",
                "clave-segura",
                "  Rosa Maria  ",
                "  Perez Soto  ",
                null,
                null,
                null
        ));

        UsuarioTenant saved = capturedUser();
        assertThat(saved.getNombres()).isEqualTo("Rosa Maria");
        assertThat(saved.getApellidos()).isEqualTo("Perez Soto");
    }

    @Test
    void apellidosEnBlancoSeGuardaComoNulo() {
        stubHappyPath();

        useCase.execute(new CreateUsuarioTenantRequest(
                "vendedor2",
                "clave-segura",
                "Rosa Maria",
                "   ",
                null,
                null,
                null
        ));

        assertThat(capturedUser().getApellidos()).isNull();
    }

    private void stubHappyPath() {
        when(usuarioTenantRepository.existsByUsernameIgnoreCase("vendedor2")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        Rol rol = new Rol();
        rol.setCodigo("VENDEDOR");
        when(rolRepository.findByCodigoIgnoreCase("VENDEDOR")).thenReturn(Optional.of(rol));
        when(usuarioTenantRepository.save(any(UsuarioTenant.class))).thenAnswer(invocation -> {
            UsuarioTenant usuario = invocation.getArgument(0);
            usuario.setId(9L);
            return usuario;
        });
        when(usuarioSucursalScopeService.findByUsuarioId(anyLong())).thenReturn(List.of());
    }

    private UsuarioTenant capturedUser() {
        ArgumentCaptor<UsuarioTenant> captor = ArgumentCaptor.forClass(UsuarioTenant.class);
        org.mockito.Mockito.verify(usuarioTenantRepository).save(captor.capture());
        return captor.getValue();
    }
}
