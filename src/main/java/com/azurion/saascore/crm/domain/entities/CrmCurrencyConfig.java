package com.azurion.saascore.crm.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "crm_currency_config")
public class CrmCurrencyConfig extends BaseEntity {

    @Column(name = "moneda", nullable = false, unique = true, length = 3)
    private String moneda;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @Column(name = "simbolo", nullable = false, length = 8)
    private String simbolo;

    @Column(name = "tipo_cambio_base", nullable = false, precision = 18, scale = 6)
    private BigDecimal tipoCambioBase = BigDecimal.ONE;

    @Column(name = "margen_conversion_porcentaje", nullable = false, precision = 8, scale = 4)
    private BigDecimal margenConversionPorcentaje = BigDecimal.ZERO;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;
}
