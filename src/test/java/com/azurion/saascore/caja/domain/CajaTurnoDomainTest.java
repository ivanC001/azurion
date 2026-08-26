package com.azurion.saascore.caja.domain;

import com.azurion.saascore.caja.domain.entities.CajaTurno;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CajaTurnoDomainTest {

    @Test
    @DisplayName("Cierre de caja: calculates exact difference (positive when surplus, negative when shortage)")
    void calculateDiferenciaCierre_surplusAndShortage() {
        CajaTurno turno = new CajaTurno();
        turno.setSaldoApertura(new BigDecimal("100.00"));
        turno.setTotalVentas(new BigDecimal("450.00"));
        turno.setSaldoEsperado(new BigDecimal("550.00"));

        // Case 1: Exact match
        turno.setConteoFisico(new BigDecimal("550.00"));
        turno.setDiferenciaCierre(turno.getConteoFisico().subtract(turno.getSaldoEsperado()));
        assertThat(turno.getDiferenciaCierre()).isEqualByComparingTo("0.00");

        // Case 2: Shortage (faltante de S/ 20.00)
        turno.setConteoFisico(new BigDecimal("530.00"));
        turno.setDiferenciaCierre(turno.getConteoFisico().subtract(turno.getSaldoEsperado()));
        assertThat(turno.getDiferenciaCierre()).isEqualByComparingTo("-20.00");

        // Case 3: Surplus (sobrante de S/ 15.50)
        turno.setConteoFisico(new BigDecimal("565.50"));
        turno.setDiferenciaCierre(turno.getConteoFisico().subtract(turno.getSaldoEsperado()));
        assertThat(turno.getDiferenciaCierre()).isEqualByComparingTo("15.50");
    }

    @Test
    @DisplayName("CajaTurno defaults are initialized correctly")
    void defaultValues_initializedProperly() {
        CajaTurno turno = new CajaTurno();
        assertThat(turno.getNumeroVentas()).isEqualTo(0);
        assertThat(turno.getTotalVentas()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
