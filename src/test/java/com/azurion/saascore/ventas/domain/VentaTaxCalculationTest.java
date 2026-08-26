package com.azurion.saascore.ventas.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

class VentaTaxCalculationTest {

    private static final BigDecimal IGV_RATE = new BigDecimal("0.18");
    private static final BigDecimal ONE_PLUS_IGV = BigDecimal.ONE.add(IGV_RATE);

    @Test
    @DisplayName("IGV calculation for Gravada items (total S/ 118.00 yields base S/ 100.00 and IGV S/ 18.00)")
    void calculateTax_standardGravada_correctBreakdown() {
        BigDecimal total = new BigDecimal("118.00");
        BigDecimal valorVenta = total.divide(ONE_PLUS_IGV, 2, RoundingMode.HALF_UP);
        BigDecimal igv = total.subtract(valorVenta);

        assertThat(valorVenta).isEqualByComparingTo("100.00");
        assertThat(igv).isEqualByComparingTo("18.00");
        assertThat(valorVenta.add(igv)).isEqualByComparingTo(total);
    }

    @Test
    @DisplayName("IGV calculation with fractions and standard Peruvian rounding")
    void calculateTax_fractionalAmounts_roundedProperly() {
        BigDecimal total = new BigDecimal("25.50");
        BigDecimal valorVenta = total.divide(ONE_PLUS_IGV, 2, RoundingMode.HALF_UP);
        BigDecimal igv = total.subtract(valorVenta);

        assertThat(valorVenta).isEqualByComparingTo("21.61");
        assertThat(igv).isEqualByComparingTo("3.89");
        assertThat(valorVenta.add(igv)).isEqualByComparingTo("25.50");
    }

    @Test
    @DisplayName("Total sum of multiple line items equals header total")
    void lineItems_sumMatchesHeaderTotal() {
        BigDecimal item1 = new BigDecimal("50.00");
        BigDecimal item2 = new BigDecimal("75.30");
        BigDecimal item3 = new BigDecimal("12.20");

        BigDecimal total = item1.add(item2).add(item3);
        assertThat(total).isEqualByComparingTo("137.50");
    }
}
