package com.azurion.saascore.caja.application.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.saascore.caja.application.services.CajaActorService;
import com.azurion.saascore.caja.application.services.CajaTurnoService;
import com.azurion.saascore.caja.domain.entities.CajaMovimiento;
import com.azurion.saascore.caja.domain.entities.CajaTurno;
import com.azurion.saascore.caja.domain.repositories.CajaMovimientoRepository;
import com.azurion.shared.exception.BusinessException;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CajaMovimientoServiceTest {

    @Mock
    private CajaMovimientoRepository movimientoRepository;

    @Mock
    private CajaActorService actorService;

    @Mock
    private CajaTurnoService turnoService;

    private CajaMovimientoService service;

    @BeforeEach
    void setUp() {
        service = new CajaMovimientoService(movimientoRepository, actorService, turnoService);
    }

    @Test
    void ventaEnEfectivoIncrementaElSaldoEsperado() {
        stubSuccessfulPersistence();
        CajaTurno turno = turnoAbierto("100.00");

        CajaMovimiento movimiento = registrarVenta(turno, "EFECTIVO", "35.50");

        assertTrue(movimiento.isAfectaEfectivo());
        assertEquals(new BigDecimal("135.50"), turno.getSaldoEsperado());
        assertEquals(new BigDecimal("35.50"), turno.getTotalEfectivo());
        assertEquals(new BigDecimal("35.50"), turno.getTotalVentas());
        assertEquals(1, turno.getNumeroVentas());
        verify(turnoService).requireOpen(turno);
    }

    @Test
    void ventaConYapeSeReportaPeroNoIncrementaElEfectivo() {
        stubSuccessfulPersistence();
        CajaTurno turno = turnoAbierto("100.00");

        CajaMovimiento movimiento = registrarVenta(turno, "YAPE", "35.50");

        assertFalse(movimiento.isAfectaEfectivo());
        assertEquals(new BigDecimal("100.00"), movimiento.getSaldoResultante());
        assertEquals(new BigDecimal("100.00"), turno.getSaldoEsperado());
        assertEquals(new BigDecimal("35.50"), turno.getTotalBilleteraDigital());
        assertEquals(new BigDecimal("35.50"), turno.getTotalVentas());
    }

    @Test
    void retiroNoPuedeDejarLaCajaEnNegativo() {
        CajaTurno turno = turnoAbierto("20.00");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.registrar(
                        turno,
                        "RETIRO",
                        "MANUAL",
                        "EFECTIVO",
                        new BigDecimal("20.01"),
                        "Retiro",
                        null,
                        null,
                        null,
                        null,
                        null
                )
        );

        assertEquals("SALDO_CAJA_INSUFICIENTE", exception.getCode());
        assertEquals(new BigDecimal("20.00"), turno.getSaldoEsperado());
    }

    private CajaMovimiento registrarVenta(CajaTurno turno, String medioPago, String monto) {
        return service.registrar(
                turno,
                "VENTA",
                "VENTA",
                medioPago,
                new BigDecimal(monto),
                "Venta POS",
                "V-1",
                null,
                1L,
                null,
                null
        );
    }

    private void stubSuccessfulPersistence() {
        when(actorService.actual()).thenReturn(new CajaActorService.Actor(7L, "7", "Cajero"));
        when(movimientoRepository.save(any(CajaMovimiento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private CajaTurno turnoAbierto(String saldo) {
        CajaTurno turno = new CajaTurno();
        turno.setEstado(CajaTurnoService.ABIERTO);
        turno.setSaldoEsperado(new BigDecimal(saldo));
        return turno;
    }
}
