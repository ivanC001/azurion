package com.azurion.saascore.auth.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.saascore.auth.application.dto.ChangeCurrentUserPasswordRequest;
import com.azurion.saascore.auth.application.dto.UpdateCurrentUserProfileRequest;
import com.azurion.saascore.auth.domain.repositories.UsuarioGlobalRepository;
import com.azurion.saascore.auth.infrastructure.storage.UserProfilePhotoStorageService;
import com.azurion.saascore.usuarios.application.dto.UsuarioSucursalResponse;
import com.azurion.saascore.usuarios.application.services.UsuarioSucursalScopeService;
import com.azurion.saascore.usuarios.domain.entities.UsuarioTenant;
import com.azurion.saascore.usuarios.domain.repositories.UsuarioTenantRepository;
import com.azurion.security.jwt.TenantAuthenticationDetails;
import com.azurion.shared.exception.BusinessException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

class CurrentUserProfileServiceTest {

    private final UsuarioTenantRepository tenantUsers = mock(UsuarioTenantRepository.class);
    private final UsuarioGlobalRepository globalUsers = mock(UsuarioGlobalRepository.class);
    private final UsuarioSucursalScopeService userBranches = mock(UsuarioSucursalScopeService.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final UserProfilePhotoStorageService profilePhotoStorage = mock(UserProfilePhotoStorageService.class);
    private final CurrentUserProfileService service = new CurrentUserProfileService(
            tenantUsers,
            globalUsers,
            userBranches,
            passwordEncoder,
            profilePhotoStorage
    );

    @Test
    void updatesOnlyTheCurrentTenantUsersName() {
        UsuarioTenant user = tenantUser(18L);
        when(tenantUsers.findById(18L)).thenReturn(Optional.of(user));
        when(tenantUsers.save(user)).thenReturn(user);
        when(userBranches.findByUsuarioId(18L)).thenReturn(List.of(
                new UsuarioSucursalResponse(4L, "PRINCIPAL", "Sucursal Principal")
        ));

        var response = service.update(
                authentication(18L, "ROLE_CRM_VENDEDOR"),
                new UpdateCurrentUserProfileRequest(
                        "  Ana  ", " Diaz ", "+51 999 999 999", "Asesora comercial", "ana@old.example.com"
                )
        );

        assertThat(response.nombres()).isEqualTo("Ana");
        assertThat(response.apellidos()).isEqualTo("Diaz");
        assertThat(response.telefono()).isEqualTo("+51 999 999 999");
        assertThat(response.cargo()).isEqualTo("Asesora comercial");
        assertThat(response.email()).isEqualTo("ana@old.example.com");
        assertThat(response.roles()).containsExactly("CRM_VENDEDOR");
        assertThat(response.sucursales()).hasSize(1);
        verify(tenantUsers).save(user);
    }

    @Test
    void rejectsEmailChangesFromTheProfileEndpoint() {
        UsuarioTenant user = tenantUser(18L);
        when(tenantUsers.findById(18L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.update(
                authentication(18L, "ROLE_CRM_VENDEDOR"),
                new UpdateCurrentUserProfileRequest("Ana", "Diaz", null, null, "otro@example.com")
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no puede modificarse");

        assertThat(user.getEmail()).isEqualTo("ana@old.example.com");
    }

    @Test
    void separatesRolesFromEffectivePermissions() {
        UsuarioTenant user = tenantUser(18L);
        when(tenantUsers.findById(18L)).thenReturn(Optional.of(user));
        when(userBranches.findByUsuarioId(18L)).thenReturn(List.of());

        var response = service.get(authentication(
                18L,
                "ROLE_CRM_VENDEDOR",
                "CRM_LEADS_READ",
                "CRM_OPPORTUNITIES_WRITE"
        ));

        assertThat(response.roles()).containsExactly("CRM_VENDEDOR");
    }

    @Test
    void requiresTheCurrentPasswordBeforeChangingIt() {
        UsuarioTenant user = tenantUser(18L);
        user.setPasswordHash("current-hash");
        when(tenantUsers.findById(18L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correcta", "current-hash")).thenReturn(true);
        when(passwordEncoder.matches("Nueva123", "current-hash")).thenReturn(false);
        when(passwordEncoder.encode("Nueva123")).thenReturn("new-hash");

        service.changePassword(
                authentication(18L, "ROLE_CRM_VENDEDOR"),
                new ChangeCurrentUserPasswordRequest("correcta", "Nueva123")
        );

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(tenantUsers).save(user);
    }

    @Test
    void rejectsPersonalDataChangesForPlatformAdministrators() {
        assertThatThrownBy(() -> service.update(
                authentication(1L, "ROLE_ADMIN_GENERAL"),
                new UpdateCurrentUserProfileRequest("No debe cambiar", null, null, null, "admin@azurion.tech")
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("seguridad de plataforma");
    }

    private UsuarioTenant tenantUser(Long id) {
        UsuarioTenant user = new UsuarioTenant();
        user.setId(id);
        user.setUsername("ana.diaz");
        user.setNombres("Ana");
        user.setEmail("ana@old.example.com");
        user.setActivo(true);
        return user;
    }

    private UsernamePasswordAuthenticationToken authentication(Long userId, String... authorities) {
        var authentication = new UsernamePasswordAuthenticationToken(
                "ana.diaz",
                null,
                java.util.Arrays.stream(authorities)
                        .map(SimpleGrantedAuthority::new)
                        .toList()
        );
        authentication.setDetails(new TenantAuthenticationDetails(
                new MockHttpServletRequest(),
                userId,
                "tenant-demo",
                "tenant-demo",
                "session-id"
        ));
        return authentication;
    }
}
