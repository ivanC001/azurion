package com.azurion.saascore.crm.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "crm_whatsapp_messages")
public class CrmWhatsappMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prospecto_id")
    private CrmProspecto prospecto;

    @Column(name = "meta_message_id", nullable = false, unique = true, length = 255)
    private String metaMessageId;

    @Column(name = "direccion", nullable = false, length = 15)
    private String direccion;

    @Column(name = "remitente", length = 80)
    private String remitente;

    @Column(name = "destinatario", length = 80)
    private String destinatario;

    @Column(name = "tipo_mensaje", nullable = false, length = 40)
    private String tipoMensaje;

    @Column(name = "contenido", columnDefinition = "TEXT")
    private String contenido;

    @Column(name = "estado", nullable = false, length = 30)
    private String estado;

    @Column(name = "mensaje_en")
    private OffsetDateTime mensajeEn;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "leido_en")
    private OffsetDateTime leidoEn;

    @Column(name = "enviado_por_usuario_id", length = 120)
    private String enviadoPorUsuarioId;

    @Column(name = "enviado_por_nombre", length = 160)
    private String enviadoPorNombre;

    @Column(name = "error_codigo", length = 80)
    private String errorCodigo;

    @Column(name = "error_detalle", length = 500)
    private String errorDetalle;

    @Column(name = "plantilla_nombre", length = 512)
    private String plantillaNombre;

    @Column(name = "plantilla_idioma", length = 35)
    private String plantillaIdioma;

    @Column(name = "plantilla_parametros_json", columnDefinition = "TEXT")
    private String plantillaParametrosJson;

    public String getPlantillaNombre() { return plantillaNombre; }
    public void setPlantillaNombre(String value) { plantillaNombre = value; }
    public String getPlantillaIdioma() { return plantillaIdioma; }
    public void setPlantillaIdioma(String value) { plantillaIdioma = value; }
    public String getPlantillaParametrosJson() { return plantillaParametrosJson; }
    public void setPlantillaParametrosJson(String value) { plantillaParametrosJson = value; }

    public CrmProspecto getProspecto() {
        return prospecto;
    }

    public void setProspecto(CrmProspecto prospecto) {
        this.prospecto = prospecto;
    }

    public String getMetaMessageId() {
        return metaMessageId;
    }

    public void setMetaMessageId(String metaMessageId) {
        this.metaMessageId = metaMessageId;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getRemitente() {
        return remitente;
    }

    public void setRemitente(String remitente) {
        this.remitente = remitente;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(String destinatario) {
        this.destinatario = destinatario;
    }

    public String getTipoMensaje() {
        return tipoMensaje;
    }

    public void setTipoMensaje(String tipoMensaje) {
        this.tipoMensaje = tipoMensaje;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public OffsetDateTime getMensajeEn() {
        return mensajeEn;
    }

    public void setMensajeEn(OffsetDateTime mensajeEn) {
        this.mensajeEn = mensajeEn;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }

    public OffsetDateTime getLeidoEn() {
        return leidoEn;
    }

    public void setLeidoEn(OffsetDateTime leidoEn) {
        this.leidoEn = leidoEn;
    }

    public String getEnviadoPorUsuarioId() {
        return enviadoPorUsuarioId;
    }

    public void setEnviadoPorUsuarioId(String enviadoPorUsuarioId) {
        this.enviadoPorUsuarioId = enviadoPorUsuarioId;
    }

    public String getEnviadoPorNombre() {
        return enviadoPorNombre;
    }

    public void setEnviadoPorNombre(String enviadoPorNombre) {
        this.enviadoPorNombre = enviadoPorNombre;
    }

    public String getErrorCodigo() {
        return errorCodigo;
    }

    public void setErrorCodigo(String errorCodigo) {
        this.errorCodigo = errorCodigo;
    }

    public String getErrorDetalle() {
        return errorDetalle;
    }

    public void setErrorDetalle(String errorDetalle) {
        this.errorDetalle = errorDetalle;
    }
}
