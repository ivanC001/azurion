package com.azurion.saascore.crm.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "crm_prospecto_intereses")
public class CrmProspectoInteres extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prospecto_id", nullable = false)
    private CrmProspecto prospecto;

    @Column(name = "landing_key", length = 120)
    private String landingKey;

    @Column(name = "campania", length = 120)
    private String campania;

    @Column(name = "canal_ingreso", nullable = false, length = 30)
    private String canalIngreso = "LANDING";

    @Column(name = "catalogo_item_id")
    private Long catalogoItemId;

    @Column(name = "producto_pendiente", nullable = false)
    private boolean productoPendiente = false;

    @Column(name = "tipo_interes", nullable = false, length = 30)
    private String tipoInteres = "PRODUCTO";

    @Column(name = "interes_principal", length = 220)
    private String interesPrincipal;

    @Column(name = "interes_detalle", length = 1500)
    private String interesDetalle;

    @Column(name = "mensaje", length = 1500)
    private String mensaje;

    @Column(name = "presupuesto_estimado", precision = 18, scale = 2)
    private BigDecimal presupuestoEstimado;

    @Column(name = "fecha_interes")
    private LocalDate fechaInteres;

    @Column(name = "landing_url", length = 500)
    private String landingUrl;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "contador_envios", nullable = false)
    private Integer contadorEnvios = 1;

    @Column(name = "ultimo_envio_en", nullable = false)
    private OffsetDateTime ultimoEnvioEn = OffsetDateTime.now();

    public CrmProspecto getProspecto() {
        return prospecto;
    }

    public void setProspecto(CrmProspecto prospecto) {
        this.prospecto = prospecto;
    }

    public String getLandingKey() {
        return landingKey;
    }

    public void setLandingKey(String landingKey) {
        this.landingKey = landingKey;
    }

    public String getCampania() {
        return campania;
    }

    public void setCampania(String campania) {
        this.campania = campania;
    }

    public String getCanalIngreso() {
        return canalIngreso;
    }

    public void setCanalIngreso(String canalIngreso) {
        this.canalIngreso = canalIngreso;
    }

    public Long getCatalogoItemId() {
        return catalogoItemId;
    }

    public void setCatalogoItemId(Long catalogoItemId) {
        this.catalogoItemId = catalogoItemId;
    }

    public boolean isProductoPendiente() {
        return productoPendiente;
    }

    public void setProductoPendiente(boolean productoPendiente) {
        this.productoPendiente = productoPendiente;
    }

    public String getTipoInteres() {
        return tipoInteres;
    }

    public void setTipoInteres(String tipoInteres) {
        this.tipoInteres = tipoInteres;
    }

    public String getInteresPrincipal() {
        return interesPrincipal;
    }

    public void setInteresPrincipal(String interesPrincipal) {
        this.interesPrincipal = interesPrincipal;
    }

    public String getInteresDetalle() {
        return interesDetalle;
    }

    public void setInteresDetalle(String interesDetalle) {
        this.interesDetalle = interesDetalle;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public BigDecimal getPresupuestoEstimado() {
        return presupuestoEstimado;
    }

    public void setPresupuestoEstimado(BigDecimal presupuestoEstimado) {
        this.presupuestoEstimado = presupuestoEstimado;
    }

    public LocalDate getFechaInteres() {
        return fechaInteres;
    }

    public void setFechaInteres(LocalDate fechaInteres) {
        this.fechaInteres = fechaInteres;
    }

    public String getLandingUrl() {
        return landingUrl;
    }

    public void setLandingUrl(String landingUrl) {
        this.landingUrl = landingUrl;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public Integer getContadorEnvios() {
        return contadorEnvios;
    }

    public void setContadorEnvios(Integer contadorEnvios) {
        this.contadorEnvios = contadorEnvios;
    }

    public OffsetDateTime getUltimoEnvioEn() {
        return ultimoEnvioEn;
    }

    public void setUltimoEnvioEn(OffsetDateTime ultimoEnvioEn) {
        this.ultimoEnvioEn = ultimoEnvioEn;
    }
}
