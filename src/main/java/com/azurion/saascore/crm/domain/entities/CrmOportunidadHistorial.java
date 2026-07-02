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
@Table(name = "crm_oportunidad_historial")
public class CrmOportunidadHistorial extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "oportunidad_id", nullable = false)
    private CrmOportunidad oportunidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etapa_origen_id")
    private CrmEtapaPipeline etapaOrigen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etapa_destino_id", nullable = false)
    private CrmEtapaPipeline etapaDestino;

    @Column(name = "usuario_id", nullable = false, length = 80)
    private String usuarioId;

    @Column(name = "observacion", length = 500)
    private String observacion;

    @Column(name = "fecha_cambio", nullable = false)
    private OffsetDateTime fechaCambio = OffsetDateTime.now();
}
