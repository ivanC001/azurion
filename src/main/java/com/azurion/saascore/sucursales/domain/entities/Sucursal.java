package com.azurion.saascore.sucursales.domain.entities;

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
@Table(name = "sucursales")
public class Sucursal extends BaseEntity {

    @Column(name = "codigo", nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "direccion", length = 255)
    private String direccion;

    @Column(name = "ubigeo_codigo", nullable = false, length = 6)
    private String ubigeoCodigo;

    @Column(name = "departamento", nullable = false, length = 120)
    private String departamento;

    @Column(name = "provincia", nullable = false, length = 120)
    private String provincia;

    @Column(name = "distrito", nullable = false, length = 160)
    private String distrito;

    @Column(name = "igv_porcentaje", nullable = false, precision = 5, scale = 2)
    private BigDecimal igvPorcentaje = new BigDecimal("18.00");

    @Column(name = "tipo_operacion_default_id", length = 4)
    private String tipoOperacionDefaultId;

    @Column(name = "tipo_afectacion_default_id", length = 4)
    private String tipoAfectacionDefaultId;

    @Column(name = "tributo_default_id", length = 6)
    private String tributoDefaultId;

    @Column(name = "porcentaje_igv_default", precision = 5, scale = 2)
    private BigDecimal porcentajeIgvDefault;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;
}
