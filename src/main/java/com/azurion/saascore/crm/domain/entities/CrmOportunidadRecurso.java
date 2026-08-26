package com.azurion.saascore.crm.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "crm_oportunidad_recursos")
public class CrmOportunidadRecurso extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "oportunidad_id", nullable = false)
    private CrmOportunidad oportunidad;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(name = "external_key", length = 180)
    private String externalKey;

    @Column(name = "data_json", nullable = false, columnDefinition = "TEXT")
    private String dataJson = "{}";

    @Column(name = "archivo_nombre", length = 255)
    private String archivoNombre;

    @Column(name = "archivo_path", length = 700)
    private String archivoPath;

    @Column(name = "archivo_mime_type", length = 120)
    private String archivoMimeType;

    @Column(name = "archivo_size")
    private Long archivoSize;

    @Column(name = "created_by", nullable = false, length = 160)
    private String createdBy;

    public CrmOportunidad getOportunidad() {
        return oportunidad;
    }

    public void setOportunidad(CrmOportunidad oportunidad) {
        this.oportunidad = oportunidad;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getExternalKey() {
        return externalKey;
    }

    public void setExternalKey(String externalKey) {
        this.externalKey = externalKey;
    }

    public String getDataJson() {
        return dataJson;
    }

    public void setDataJson(String dataJson) {
        this.dataJson = dataJson;
    }

    public String getArchivoNombre() {
        return archivoNombre;
    }

    public void setArchivoNombre(String archivoNombre) {
        this.archivoNombre = archivoNombre;
    }

    public String getArchivoPath() {
        return archivoPath;
    }

    public void setArchivoPath(String archivoPath) {
        this.archivoPath = archivoPath;
    }

    public String getArchivoMimeType() {
        return archivoMimeType;
    }

    public void setArchivoMimeType(String archivoMimeType) {
        this.archivoMimeType = archivoMimeType;
    }

    public Long getArchivoSize() {
        return archivoSize;
    }

    public void setArchivoSize(Long archivoSize) {
        this.archivoSize = archivoSize;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
