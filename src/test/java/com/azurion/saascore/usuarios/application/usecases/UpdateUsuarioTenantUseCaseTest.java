package com.azurion.saascore.usuarios.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.azurion.saascore.usuarios.application.dto.UpdateUsuarioTenantRequest;
import com.azurion.saascore.usuarios.application.services.TenantUserLimitService;
import com.azurion.saascore.usuarios.application.services.UsuarioSucursalScopeService;
import com.azurion.saascore.usuarios.domain.entities.UsuarioTenant;
import com.azurion.saascore.usuarios.domain.repositories.UsuarioTenantRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateUsuarioTenantUseCaseTest {

    @Mock
    private UsuarioTenantRepository usuarioTenantRepository;
    @Mock
    private UsuarioSucursalScopeService usuarioSucursalScopeService;
    @Mock
    private TenantUserLimitService tenantUserLimitService;

    private UpdateUsuarioTenantUseCase useCase;

    private UsuarioTenant usuario;

    @BeforeEach
    void setUp() {
        useCase = new UpdateUsuarioTenantUseCase(
                usuarioTenantRepository,
                usuarioSucursalScopeService,
                tenantUserLimitService
        );
        usuario = new UsuarioTenant();
        usuario.setId(9L);
        usuario.setUsername("vendedor2");
        usuario.setNombres("Rosa Maria");
        usuario.setApellidos("Perez Soto");
        usuario.setActivo(true);
        when(usuarioTenantRepository.findWithUsuarioRolesById(9L)).thenReturn(Optional.of(usuario));
        when(usuarioTenantRepository.save(any(UsuarioTenant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(usuarioSucursalScopeService.findByUsuarioId(anyLong())).thenReturn(List.of());
    }

    @Test
    void actualizaApellidosRecortados() {
        useCase.execute(9L, new UpdateUsuarioTenantRequest("Rosa Maria", "  Soto Vega  ", null, null, null));

        assertThat(usuario.getApellidos()).isEqualTo("Soto Vega");
    }

    @Test
    void apellidosNuloNoPisaElValorGuardado() {
        useCase.execute(9L, new UpdateUsuarioTenantRequest("Rosa Maria", null, null, null, null));

        assertThat(usuario.getApellidos()).isEqualTo("Perez Soto");
    }

    @Test
    void apellidosEnBlancoLimpiaElValorGuardado() {
        useCase.execute(9L, new UpdateUsuarioTenantRequest("Rosa Maria", "   ", null, null, null));

        assertThat(usuario.getApellidos()).isNull();
    }
}
