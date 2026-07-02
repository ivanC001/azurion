package com.azurion.saascore.caja.application.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.azurion.saascore.almacenes.domain.entities.Almacen;
import com.azurion.saascore.almacenes.domain.repositories.AlmacenRepository;
import com.azurion.saascore.caja.domain.entities.Caja;
import com.azurion.saascore.sucursales.domain.entities.Sucursal;
import com.azurion.shared.exception.BusinessException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VentaSucursalStockPolicyTest {

    @Mock
    private AlmacenRepository almacenRepository;

    private VentaSucursalStockPolicy policy;
    private Caja caja;

    @BeforeEach
    void setUp() {
        policy = new VentaSucursalStockPolicy(almacenRepository);
        Sucursal sucursal = new Sucursal();
        sucursal.setId(10L);
        caja = new Caja();
        caja.setSucursal(sucursal);
    }

    @Test
    void permiteVenderStockDeLaSucursalDeLaCaja() {
        when(almacenRepository.findById(20L)).thenReturn(Optional.of(almacen(20L, 10L)));

        assertDoesNotThrow(() -> policy.validar(caja, List.of(20L)));
    }

    @Test
    void rechazaVenderStockDeOtraSucursal() {
        when(almacenRepository.findById(20L)).thenReturn(Optional.of(almacen(20L, 11L)));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> policy.validar(caja, List.of(20L))
        );

        assertEquals("STOCK_OTRA_SUCURSAL", exception.getCode());
    }

    @Test
    void rechazaAlmacenSinSucursal() {
        Almacen almacen = almacen(20L, null);
        when(almacenRepository.findById(20L)).thenReturn(Optional.of(almacen));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> policy.validar(caja, List.of(20L))
        );

        assertEquals("STOCK_OTRA_SUCURSAL", exception.getCode());
    }

    private Almacen almacen(Long id, Long sucursalId) {
        Almacen almacen = new Almacen();
        almacen.setId(id);
        almacen.setNombre("Almacen " + id);
        almacen.setActivo(true);
        almacen.setEstado("ACTIVO");
        if (sucursalId != null) {
            Sucursal sucursal = new Sucursal();
            sucursal.setId(sucursalId);
            almacen.setSucursal(sucursal);
        }
        return almacen;
    }
}
