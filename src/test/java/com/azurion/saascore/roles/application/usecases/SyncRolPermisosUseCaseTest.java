package com.azurion.saascore.roles.application.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.saascore.roles.domain.entities.Rol;
import com.azurion.saascore.roles.domain.repositories.PermisoRepository;
import com.azurion.saascore.roles.domain.repositories.RolRepository;
import com.azurion.shared.exception.BusinessException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SyncRolPermisosUseCaseTest {

    @Mock
    private RolRepository rolRepository;

    @Mock
    private PermisoRepository permisoRepository;

    @InjectMocks
    private SyncRolPermisosUseCase useCase;

    @Test
    void shouldRejectPermissionSyncForAdministrativeRole() {
        Rol rol = new Rol();
        rol.setId(1L);
        rol.setCodigo("ADMIN_EMPRESA");
        rol.setSistema(true);
        when(rolRepository.findWithPermisosById(1L)).thenReturn(Optional.of(rol));

        BusinessException exception = assertThrows(BusinessException.class, () -> useCase.execute(1L, List.of(3L, 4L)));

        assertEquals("ROL_RESERVADO", exception.getCode());
        verify(permisoRepository, never()).findByIdIn(List.of(3L, 4L));
    }
}
