package com.azurion.saascore.crm.domain.entities;

import com.azurion.saascore.clientes.domain.entities.Cliente;
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
@Table(name = "crm_actividades")
public class CrmActividad extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prospecto_id")
    private CrmProspecto prospecto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oportunidad_id")
    private CrmOportunidad oportunidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(name = "tipo_actividad", nullable = false, length = 30)
    private String tipoActividad;

    @Column(name = "asunto", nullable = false, length = 220)
    private String asunto;

    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    @Column(name = "fecha_programada", nullable = false)
    private OffsetDateTime fechaProgramada;

    @Column(name = "fecha_realizada")
    private OffsetDateTime fechaRealizada;

    @Column(name = "estado", nullable = false, length = 30)
    private String estado = "PENDIENTE";

    @Column(name = "usuario_id", nullable = false, length = 80)
    private String usuarioId;

    @Column(name = "resultado", length = 1000)
    private String resultado;

    @Column(name = "resultado_contacto", length = 40)
    private String resultadoContacto;

    @Column(name = "nivel_interes", length = 20)
    private String nivelInteres;

    @Column(name = "estado_prospecto_resultado", length = 30)
    private String estadoProspectoResultado;
}
