package com.azurion.saascore.roles.application.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.saascore.roles.domain.entities.Permiso;
import com.azurion.saascore.roles.domain.repositories.PermisoRepository;
import com.azurion.saascore.roles.domain.repositories.RolPermisoRepository;
import com.azurion.shared.exception.BusinessException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeletePermisoUseCaseTest {

    @Mock
    private PermisoRepository permisoRepository;

    @Mock
    private RolPermisoRepository rolPermisoRepository;

    @InjectMocks
    private DeletePermisoUseCase useCase;

    @Test
    void shouldRejectSystemPermissionDeletion() {
        Permiso permiso = new Permiso();
        permiso.setId(12L);
        permiso.setCodigo("ROLES_WRITE");
        permiso.setSistema(true);
        when(permisoRepository.findById(12L)).thenReturn(Optional.of(permiso));

        BusinessException exception = assertThrows(BusinessException.class, () -> useCase.execute(12L));

        assertEquals("PERMISO_RESERVADO", exception.getCode());
        verify(rolPermisoRepository, never()).countByPermiso_Id(12L);
        verify(permisoRepository, never()).deleteById(12L);
    }
}
