package com.azurion.saascore.sucursales.application.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.saascore.almacenes.application.dto.CreateAlmacenRequest;
import com.azurion.saascore.almacenes.application.services.OperationalCodeGenerator;
import com.azurion.saascore.almacenes.application.usecases.CreateAlmacenUseCase;
import com.azurion.saascore.sucursales.application.dto.CreateSucursalRequest;
import com.azurion.saascore.sucursales.application.dto.SucursalResponse;
import com.azurion.saascore.sucursales.domain.entities.Sucursal;
import com.azurion.saascore.sucursales.domain.repositories.SucursalRepository;
import com.azurion.saascore.ubigeos.domain.entities.Ubigeo;
import com.azurion.saascore.sucursales.application.services.SucursalLocationResolver;
import com.azurion.saascore.sucursales.application.services.SucursalLocationResolver.SucursalLocation;
import com.azurion.shared.persistence.BusinessOperationLockService;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateSucursalUseCaseTest {

    @Mock
    private SucursalRepository sucursalRepository;
    @Mock
    private SucursalLocationResolver locationResolver;
    @Mock
    private OperationalCodeGenerator codeGenerator;
    @Mock
    private CreateAlmacenUseCase createAlmacenUseCase;
    @Mock
    private BusinessOperationLockService operationLockService;

    private CreateSucursalUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateSucursalUseCase(
                sucursalRepository,
                locationResolver,
                codeGenerator,
                createAlmacenUseCase,
                operationLockService
        );
    }

    @Test
    void generaCodigoYPuedeCrearElAlmacenPrincipalEnLaMismaOperacion() {
        when(codeGenerator.nextSucursalCode()).thenReturn("SUC-001");
        when(locationResolver.resolve("150101", null, null, null))
                .thenReturn(new SucursalLocation("150101", "LIMA", "LIMA", "LIMA"));
        when(sucursalRepository.save(any(Sucursal.class))).thenAnswer(invocation -> {
            Sucursal sucursal = invocation.getArgument(0);
            sucursal.setId(10L);
            return sucursal;
        });

        SucursalResponse response = useCase.execute(new CreateSucursalRequest(
                null,
                "Sucursal Norte",
                "Av. Norte 100",
                "150101",
                null,
                null,
                null,
                new BigDecimal("18.00"),
                true
        ));

        assertEquals("SUC-001", response.codigo());
        ArgumentCaptor<CreateAlmacenRequest> requestCaptor = ArgumentCaptor.forClass(CreateAlmacenRequest.class);
        verify(createAlmacenUseCase).execute(requestCaptor.capture());
        assertEquals(10L, requestCaptor.getValue().sucursalId());
        assertEquals("PRINCIPAL", requestCaptor.getValue().tipoAlmacen());
        assertEquals("Av. Norte 100", requestCaptor.getValue().direccion());
    }
}
