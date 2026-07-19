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
}
