package com.azurion.saascore.caja.application.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RegistrarVentaCajaTaxCalculationTest {

    @Test
    void precioFinalCienSeSeparaSinCrearCentimosExtra() {
        BigDecimal total = new BigDecimal("100.00");
        BigDecimal porcentaje = new BigDecimal("18.00");

        BigDecimal base = RegistrarVentaCajaUseCase.calculateLineBase(total, "10", porcentaje);
        BigDecimal igv = RegistrarVentaCajaUseCase.calculateLineIgv(total, base, "10", porcentaje);

        assertEquals(new BigDecimal("84.75"), base);
        assertEquals(new BigDecimal("15.25"), igv);
        assertEquals(total, base.add(igv));
    }

    @Test
    void precioFinalCientoDieciochoContieneDieciochoDeIgv() {
        BigDecimal total = new BigDecimal("118.00");
        BigDecimal porcentaje = new BigDecimal("18.00");

        BigDecimal base = RegistrarVentaCajaUseCase.calculateLineBase(total, "10", porcentaje);
        BigDecimal igv = RegistrarVentaCajaUseCase.calculateLineIgv(total, base, "10", porcentaje);

        assertEquals(new BigDecimal("100.00"), base);
        assertEquals(new BigDecimal("18.00"), igv);
        assertEquals(total, base.add(igv));
    }

    @Test
    void productoExoneradoMantienePrecioFinalSinIgv() {
        BigDecimal total = new BigDecimal("100.00");
        BigDecimal porcentaje = BigDecimal.ZERO;

        BigDecimal base = RegistrarVentaCajaUseCase.calculateLineBase(total, "20", porcentaje);
        BigDecimal igv = RegistrarVentaCajaUseCase.calculateLineIgv(total, base, "20", porcentaje);

        assertEquals(new BigDecimal("100.00"), base);
        assertEquals(new BigDecimal("0.00"), igv);
        assertEquals(total, base.add(igv));
    }
}
