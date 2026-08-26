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
@Table(name = "crm_metas")
public class CrmMeta extends BaseEntity {

    @Column(name = "anio", nullable = false)
    private Integer anio;

    @Column(name = "mes", nullable = false)
    private Integer mes;

    @Column(name = "alcance", nullable = false, length = 20)
    private String alcance;

    @Column(name = "responsable_id", length = 80)
    private String responsableId;

    @Column(name = "moneda", nullable = false, length = 3)
    private String moneda;

    @Column(name = "meta_ingresos", nullable = false, precision = 18, scale = 2)
    private BigDecimal metaIngresos = BigDecimal.ZERO;

    @Column(name = "meta_oportunidades_ganadas", nullable = false)
    private Integer metaOportunidadesGanadas = 0;

    @Column(name = "meta_prospectos_nuevos", nullable = false)
    private Integer metaProspectosNuevos = 0;

    @Column(name = "meta_actividades_realizadas", nullable = false)
    private Integer metaActividadesRealizadas = 0;

    @Column(name = "meta_conversion", nullable = false, precision = 5, scale = 2)
    private BigDecimal metaConversion = BigDecimal.ZERO;

    @Column(name = "created_by", nullable = false, length = 80)
    private String createdBy;
}
