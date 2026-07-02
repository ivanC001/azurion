package com.azurion.saascore.cotizaciones.domain.entities;

import com.azurion.saascore.clientes.domain.entities.Cliente;
import com.azurion.saascore.sucursales.domain.entities.Sucursal;
import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "cotizaciones")
public class Cotizacion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(name = "usuario_id", nullable = false, length = 80)
    private String usuarioId;

    @Column(name = "usuario_nombre", nullable = false, length = 150)
    private String usuarioNombre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "moneda", nullable = false, length = 3)
    private String moneda = "PEN";

    @Column(name = "subtotal", nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 18, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "BORRADOR";

    @Column(name = "observacion", length = 500)
    private String observacion;

    @Column(name = "venta_id")
    private Long ventaId;

    @Column(name = "crm_oportunidad_id")
    private Long crmOportunidadId;

    @Column(name = "fecha_envio")
    private OffsetDateTime fechaEnvio;

    @Column(name = "canal_envio", length = 30)
    private String canalEnvio;

    @Column(name = "proximo_seguimiento_en")
    private OffsetDateTime proximoSeguimientoEn;

    @Column(name = "fecha_respuesta")
    private OffsetDateTime fechaRespuesta;

    @Column(name = "motivo_rechazo", length = 500)
    private String motivoRechazo;

    @Column(name = "decision_siguiente", length = 30)
    private String decisionSiguiente;

    @Column(name = "convertida_en")
    private OffsetDateTime convertidaEn;

    @OneToMany(mappedBy = "cotizacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CotizacionDetalle> detalles = new ArrayList<>();
}
