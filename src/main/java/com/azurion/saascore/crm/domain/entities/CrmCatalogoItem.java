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
@Table(name = "crm_catalogo_items")
public class CrmCatalogoItem extends BaseEntity {

    @Column(name = "tipo_item", nullable = false, length = 30)
    private String tipoItem;

    @Column(name = "nombre", nullable = false, length = 220)
    private String nombre;

    @Column(name = "descripcion", length = 1500)
    private String descripcion;

    @Column(name = "precio_referencial", nullable = false, precision = 18, scale = 2)
    private BigDecimal precioReferencial = BigDecimal.ZERO;

    @Column(name = "moneda", nullable = false, length = 3)
    private String moneda = "PEN";

    @Column(name = "estado", nullable = false, length = 30)
    private String estado = "ACTIVO";

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "public_token", nullable = false, length = 80)
    private String publicToken;

    @Column(name = "public_enabled", nullable = false)
    private boolean publicEnabled = true;

    @Column(name = "landing_slug", length = 140)
    private String landingSlug;

    public String getTipoItem() {
        return tipoItem;
    }

    public void setTipoItem(String tipoItem) {
        this.tipoItem = tipoItem;
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

    public BigDecimal getPrecioReferencial() {
        return precioReferencial;
    }

    public void setPrecioReferencial(BigDecimal precioReferencial) {
        this.precioReferencial = precioReferencial;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public String getPublicToken() {
        return publicToken;
    }

    public void setPublicToken(String publicToken) {
        this.publicToken = publicToken;
    }

    public boolean isPublicEnabled() {
        return publicEnabled;
    }

    public void setPublicEnabled(boolean publicEnabled) {
        this.publicEnabled = publicEnabled;
    }

    public String getLandingSlug() {
        return landingSlug;
    }

    public void setLandingSlug(String landingSlug) {
        this.landingSlug = landingSlug;
    }
}
