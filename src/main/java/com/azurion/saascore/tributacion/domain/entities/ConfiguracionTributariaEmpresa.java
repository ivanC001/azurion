package com.azurion.saascore.tributacion.domain.entities;

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
@Table(name = "configuracion_tributaria_empresa")
public class ConfiguracionTributariaEmpresa extends BaseEntity {

    @Column(name = "tipo_operacion_default_id", nullable = false, length = 4)
    private String tipoOperacionDefaultId;

    @Column(name = "tipo_afectacion_default_id", nullable = false, length = 4)
    private String tipoAfectacionDefaultId;

    @Column(name = "tributo_default_id", nullable = false, length = 6)
    private String tributoDefaultId;

    @Column(name = "porcentaje_igv_default", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeIgvDefault;

    @Column(name = "moneda_default", nullable = false, length = 3)
    private String monedaDefault;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;
}
