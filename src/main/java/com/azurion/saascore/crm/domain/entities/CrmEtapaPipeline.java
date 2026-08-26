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

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public Integer getProbabilidadDefault() {
        return probabilidadDefault;
    }

    public void setProbabilidadDefault(Integer probabilidadDefault) {
        this.probabilidadDefault = probabilidadDefault;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getIcono() {
        return icono;
    }

    public void setIcono(String icono) {
        this.icono = icono;
    }

    public boolean isGanado() {
        return ganado;
    }

    public void setGanado(boolean ganado) {
        this.ganado = ganado;
    }

    public boolean isPerdido() {
        return perdido;
    }

    public void setPerdido(boolean perdido) {
        this.perdido = perdido;
    }

    public boolean isRequiereValidacion() {
        return requiereValidacion;
    }

    public void setRequiereValidacion(boolean requiereValidacion) {
        this.requiereValidacion = requiereValidacion;
    }

    public String getModoValidacion() {
        return modoValidacion;
    }

    public void setModoValidacion(String modoValidacion) {
        this.modoValidacion = modoValidacion;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
