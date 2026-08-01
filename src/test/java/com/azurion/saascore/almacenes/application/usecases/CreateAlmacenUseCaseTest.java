package com.azurion.saascore.almacenes.application.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.saascore.almacenes.application.dto.AlmacenResponse;
import com.azurion.saascore.almacenes.application.dto.CreateAlmacenRequest;
import com.azurion.saascore.almacenes.application.services.OperationalCodeGenerator;
import com.azurion.saascore.almacenes.domain.entities.Almacen;
import com.azurion.saascore.almacenes.domain.repositories.AlmacenRepository;
import com.azurion.saascore.sucursales.application.services.SucursalOperationalGuard;
import com.azurion.saascore.sucursales.domain.entities.Sucursal;
import com.azurion.saascore.sucursales.domain.repositories.SucursalRepository;
import com.azurion.shared.exception.BusinessException;
import com.azurion.shared.persistence.BusinessOperationLockService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateAlmacenUseCaseTest {

    @Mock
    private AlmacenRepository almacenRepository;
    @Mock
    private SucursalRepository sucursalRepository;
    @Mock
    private SucursalOperationalGuard sucursalOperationalGuard;
    @Mock
    private OperationalCodeGenerator codeGenerator;
    @Mock
    private BusinessOperationLockService operationLockService;

    private CreateAlmacenUseCase useCase;
    private Sucursal sucursal;

    @BeforeEach
    void setUp() {
        useCase = new CreateAlmacenUseCase(
                almacenRepository,
                sucursalRepository,
                sucursalOperationalGuard,
                codeGenerator,
                operationLockService
        );
        sucursal = new Sucursal();
        sucursal.setId(10L);
        sucursal.setCodigo("SUC-010");
        sucursal.setNombre("Sucursal Norte");
        sucursal.setDireccion("Av. Norte 100");
        sucursal.setActivo(true);
        when(sucursalRepository.findById(10L)).thenReturn(Optional.of(sucursal));
    }

    @Test
    void completaDatosYUsaSecundarioCuandoYaExistePrincipal() {
        when(almacenRepository.existsBySucursalIdAndTipoAlmacenIgnoreCaseAndActivoTrue(10L, "PRINCIPAL"))
                .thenReturn(true);
        when(codeGenerator.nextAlmacenCode(sucursal)).thenReturn("ALM-SUC-010-02");
        when(almacenRepository.findByCodigoIgnoreCase("ALM-SUC-010-02")).thenReturn(Optional.empty());
        when(almacenRepository.save(any(Almacen.class))).thenAnswer(invocation -> {
            Almacen almacen = invocation.getArgument(0);
            almacen.setId(20L);
            return almacen;
        });

        AlmacenResponse response = useCase.execute(new CreateAlmacenRequest(
                null,
                null,
                null,
                10L,
                null,
                null
        ));

        assertEquals("ALM-SUC-010-02", response.codigo());
        assertEquals("Almacen secundario - Sucursal Norte", response.nombre());
        assertEquals("Av. Norte 100", response.direccion());
        assertEquals("SECUNDARIO", response.tipoAlmacen());
        assertTrue(response.permiteVenta());
    }

    @Test
    void rechazaUnSegundoAlmacenPrincipalActivo() {
        when(almacenRepository.existsBySucursalIdAndTipoAlmacenIgnoreCaseAndActivoTrue(10L, "PRINCIPAL"))
                .thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> useCase.execute(new CreateAlmacenRequest(
                        null,
                        "Otro principal",
                        null,
                        10L,
                        "PRINCIPAL",
                        true
                ))
        );

        assertEquals("ALMACEN_PRINCIPAL_DUPLICADO", exception.getCode());
        verify(almacenRepository, never()).save(any());
    }
}
