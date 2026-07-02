package com.azurion.saascore.crm.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "crm_etapas_pipeline")
public class CrmEtapaPipeline extends BaseEntity {

    @Column(name = "codigo", nullable = false, unique = true, length = 40)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "descripcion", length = 300)
    private String descripcion;

    @Column(name = "orden", nullable = false)
    private Integer orden;

    @Column(name = "probabilidad_default", nullable = false)
    private Integer probabilidadDefault = 0;

    @Column(name = "color", nullable = false, length = 20)
    private String color = "#2563eb";

    @Column(name = "icono", length = 80)
    private String icono = "pi pi-briefcase";

    @Column(name = "es_ganado", nullable = false)
    private boolean ganado;

    @Column(name = "es_perdido", nullable = false)
    private boolean perdido;

    @Column(name = "requiere_validacion", nullable = false)
    private boolean requiereValidacion = true;

    @Column(name = "modo_validacion", nullable = false, length = 20)
    private String modoValidacion = "WARNING";

    @Column(name = "activo", nullable = false)
    private boolean activo = true;
}
