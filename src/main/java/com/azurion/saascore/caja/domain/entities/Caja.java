package com.azurion.saascore.caja.domain.entities;

import com.azurion.saascore.sucursales.domain.entities.Sucursal;
import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "cajas")
public class Caja extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @Column(name = "codigo", nullable = false, length = 50)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "saldo_capital", nullable = false, precision = 18, scale = 2)
    private BigDecimal saldoCapital;

    @Column(name = "saldo_actual", nullable = false, precision = 18, scale = 2)
    private BigDecimal saldoActual;

    @Column(name = "saldo_salida", precision = 18, scale = 2)
    private BigDecimal saldoSalida;

    @Column(name = "total_entradas", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalEntradas;

    @Column(name = "total_salidas", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalSalidas;

    @Column(name = "total_depositos", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalDepositos;

    @Column(name = "diferencia_cierre", precision = 18, scale = 2)
    private BigDecimal diferenciaCierre;

    @Column(name = "responsable_apertura_id", nullable = false, length = 80)
    private String responsableAperturaId;

    @Column(name = "responsable_apertura_nombre", nullable = false, length = 150)
    private String responsableAperturaNombre;

    @Column(name = "responsable_cierre_id", length = 80)
    private String responsableCierreId;

    @Column(name = "responsable_cierre_nombre", length = 150)
    private String responsableCierreNombre;

    @Column(name = "fecha_apertura", nullable = false)
    private OffsetDateTime fechaApertura;

    @Column(name = "fecha_cierre")
    private OffsetDateTime fechaCierre;

    @Column(name = "observacion_apertura", length = 500)
    private String observacionApertura;

    @Column(name = "observacion_cierre", length = 500)
    private String observacionCierre;
}
