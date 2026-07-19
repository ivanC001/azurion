package com.azurion.saascore.roles.application.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.saascore.roles.application.dto.CreateRolRequest;
import com.azurion.saascore.roles.domain.entities.Rol;
import com.azurion.saascore.roles.domain.entities.RoleScope;
import com.azurion.saascore.roles.domain.repositories.RolRepository;
import com.azurion.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateRolUseCaseTest {

    @Mock
    private RolRepository rolRepository;

    @InjectMocks
    private CreateRolUseCase useCase;

    @Test
    void shouldRejectReservedRoleCode() {
        CreateRolRequest request = new CreateRolRequest("admin", "Administrador", "Reservado", RoleScope.TENANT);

        BusinessException exception = assertThrows(BusinessException.class, () -> useCase.execute(request));

        assertEquals("ROL_RESERVADO", exception.getCode());
        verify(rolRepository, never()).save(any(Rol.class));
    }

    @Test
    void shouldNormalizeAndPersistCustomRole() {
        CreateRolRequest request = new CreateRolRequest(
                " supervisor ventas ", "Supervisor ventas", "Opera reportes", RoleScope.ERP
        );
        when(rolRepository.existsByCodigoIgnoreCase("SUPERVISOR_VENTAS")).thenReturn(false);
        when(rolRepository.save(any(Rol.class))).thenAnswer(invocation -> {
            Rol rol = invocation.getArgument(0);
            rol.setId(77L);
            return rol;
        });

        useCase.execute(request);

        ArgumentCaptor<Rol> captor = ArgumentCaptor.forClass(Rol.class);
        verify(rolRepository).save(captor.capture());
        Rol saved = captor.getValue();
        assertEquals("SUPERVISOR_VENTAS", saved.getCodigo());
        assertEquals("Supervisor ventas", saved.getNombre());
        assertEquals(false, saved.isSistema());
        assertEquals(RoleScope.ERP, saved.getAmbito());
    }
}
