package com.azurion.saascore.crm.domain.entities;

import com.azurion.saascore.clientes.domain.entities.Cliente;
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
@Table(name = "crm_oportunidades")
public class CrmOportunidad extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prospecto_id")
    private CrmProspecto prospecto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(name = "titulo", nullable = false, length = 220)
    private String titulo;

    @Column(name = "tipo_oportunidad", nullable = false, length = 30)
    private String tipoOportunidad = "PRODUCTO";

    @Column(name = "catalogo_item_id")
    private Long catalogoItemId;

    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    @Column(name = "monto_estimado", nullable = false, precision = 18, scale = 2)
    private BigDecimal montoEstimado = BigDecimal.ZERO;

    @Column(name = "probabilidad", nullable = false)
    private Integer probabilidad = 0;

    @Column(name = "etapa", nullable = false, length = 30)
    private String etapa = "NUEVO";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etapa_id", nullable = false)
    private CrmEtapaPipeline etapaPipeline;

    @Column(name = "fecha_cierre_estimada")
    private LocalDate fechaCierreEstimada;

    @Column(name = "responsable_id", nullable = false, length = 80)
    private String responsableId;

    @Column(name = "estado", nullable = false, length = 30)
    private String estado = "ABIERTA";

    @Column(name = "motivo_perdida", length = 500)
    private String motivoPerdida;

    @Column(name = "fecha_cierre_real")
    private OffsetDateTime fechaCierreReal;

    @Column(name = "fecha_ultima_actualizacion")
    private OffsetDateTime fechaUltimaActualizacion;

    @Column(name = "fecha_ganada")
    private OffsetDateTime fechaGanada;

    @Column(name = "fecha_perdida")
    private OffsetDateTime fechaPerdida;

    @Column(name = "monto_real", precision = 18, scale = 2)
    private BigDecimal montoReal;
}
