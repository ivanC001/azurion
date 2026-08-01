package com.azurion.saascore.almacenes.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.azurion.saascore.almacenes.domain.entities.Almacen;
import com.azurion.saascore.almacenes.domain.repositories.AlmacenRepository;
import com.azurion.saascore.sucursales.domain.entities.Sucursal;
import com.azurion.saascore.sucursales.domain.repositories.SucursalRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OperationalCodeGeneratorTest {

    @Mock
    private SucursalRepository sucursalRepository;
    @Mock
    private AlmacenRepository almacenRepository;

    private OperationalCodeGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new OperationalCodeGenerator(sucursalRepository, almacenRepository);
    }

    @Test
    void usaElPrimerCodigoDeSucursalDisponible() {
        when(sucursalRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(
                sucursal("SUC-001"),
                sucursal("SUC-003")
        ));

        assertEquals("SUC-002", generator.nextSucursalCode());
    }

    @Test
    void generaCodigoDeAlmacenConLaSucursalYCorrelativo() {
        Sucursal sucursal = sucursal("SUC-NORTE");
        when(almacenRepository.findAll()).thenReturn(List.of(
                almacen("ALM-SUC-NORTE-01"),
                almacen("ALM-SUC-NORTE-03")
        ));

        assertEquals("ALM-SUC-NORTE-02", generator.nextAlmacenCode(sucursal));
    }

    private Sucursal sucursal(String codigo) {
        Sucursal sucursal = new Sucursal();
        sucursal.setCodigo(codigo);
        return sucursal;
    }

    private Almacen almacen(String codigo) {
        Almacen almacen = new Almacen();
        almacen.setCodigo(codigo);
        return almacen;
    }
}
