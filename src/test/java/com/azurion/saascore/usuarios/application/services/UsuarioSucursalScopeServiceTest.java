package com.azurion.saascore.usuarios.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.azurion.saascore.sucursales.domain.entities.Sucursal;
import com.azurion.saascore.sucursales.domain.repositories.SucursalRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UsuarioSucursalScopeServiceTest {

    @Mock
    EntityManager entityManager;

    @Mock
    SucursalRepository sucursalRepository;

    @Mock
    Query query;

    UsuarioSucursalScopeService service;

    /** Pares (usuario, sucursal) que acabarian insertados en la tabla puente. */
    List<Long> assignedSucursalIds;

    @BeforeEach
    void setUp() {
        service = new UsuarioSucursalScopeService(entityManager, sucursalRepository);
        assignedSucursalIds = new ArrayList<>();

        lenient().when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        lenient().when(query.setParameter(anyInt(), any())).thenAnswer(invocation -> {
            // El segundo parametro de los INSERT es siempre el id de sucursal.
            if ((int) invocation.getArgument(0) == 2 && invocation.getArgument(1) instanceof Long id) {
                assignedSucursalIds.add(id);
            }
            return query;
        });
        lenient().when(query.executeUpdate()).thenReturn(1);
    }

    private Sucursal sucursal(Long id, boolean activo) {
        Sucursal sucursal = new Sucursal();
        sucursal.setId(id);
        sucursal.setCodigo("SUC-" + id);
        sucursal.setNombre("Sucursal " + id);
        sucursal.setActivo(activo);
        return sucursal;
    }

    @Test
    void usuarioSinSucursalesIndicadasHeredaLasActivas() {
        when(sucursalRepository.findAll()).thenReturn(List.of(sucursal(1L, true), sucursal(2L, true)));
        when(sucursalRepository.findAllById(any())).thenReturn(List.of(sucursal(1L, true), sucursal(2L, true)));

        service.assignInitialScope(50L, null);

        // Sin esto el usuario queda inoperativo: validarSucursal rechaza todo.
        assertEquals(List.of(1L, 2L), assignedSucursalIds);
    }

    @Test
    void usuarioSinSucursalesIndicadasIgnoraLasInactivas() {
        when(sucursalRepository.findAll())
                .thenReturn(List.of(sucursal(1L, true), sucursal(2L, false), sucursal(3L, true)));
        when(sucursalRepository.findAllById(any())).thenReturn(List.of(sucursal(1L, true), sucursal(3L, true)));

        service.assignInitialScope(50L, List.of());

        assertEquals(List.of(1L, 3L), assignedSucursalIds);
    }

    @Test
    void respetaElAlcanceCuandoSeIndicaExplicitamente() {
        when(sucursalRepository.findAllById(any())).thenReturn(List.of(sucursal(2L, true)));

        service.assignInitialScope(50L, List.of(2L));

        assertEquals(List.of(2L), assignedSucursalIds);
    }

    @Test
    void unTenantSinSucursalesNoRompeElAltaDeUsuario() {
        when(sucursalRepository.findAll()).thenReturn(List.of());
        when(sucursalRepository.findAllById(any())).thenReturn(List.of());

        service.assignInitialScope(50L, null);

        assertTrue(assignedSucursalIds.isEmpty());
    }
}
