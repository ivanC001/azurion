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

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getUbigeoCodigo() {
        return ubigeoCodigo;
    }

    public void setUbigeoCodigo(String ubigeoCodigo) {
        this.ubigeoCodigo = ubigeoCodigo;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

    public String getProvincia() {
        return provincia;
    }

    public void setProvincia(String provincia) {
        this.provincia = provincia;
    }

    public String getDistrito() {
        return distrito;
    }

    public void setDistrito(String distrito) {
        this.distrito = distrito;
    }

    public BigDecimal getIgvPorcentaje() {
        return igvPorcentaje;
    }

    public void setIgvPorcentaje(BigDecimal igvPorcentaje) {
        this.igvPorcentaje = igvPorcentaje;
    }

    public String getTipoOperacionDefaultId() {
        return tipoOperacionDefaultId;
    }

    public void setTipoOperacionDefaultId(String tipoOperacionDefaultId) {
        this.tipoOperacionDefaultId = tipoOperacionDefaultId;
    }

    public String getTipoAfectacionDefaultId() {
        return tipoAfectacionDefaultId;
    }

    public void setTipoAfectacionDefaultId(String tipoAfectacionDefaultId) {
        this.tipoAfectacionDefaultId = tipoAfectacionDefaultId;
    }

    public String getTributoDefaultId() {
        return tributoDefaultId;
    }

    public void setTributoDefaultId(String tributoDefaultId) {
        this.tributoDefaultId = tributoDefaultId;
    }

    public BigDecimal getPorcentajeIgvDefault() {
        return porcentajeIgvDefault;
    }

    public void setPorcentajeIgvDefault(BigDecimal porcentajeIgvDefault) {
        this.porcentajeIgvDefault = porcentajeIgvDefault;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
