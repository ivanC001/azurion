package com.azurion.saascore.caja.domain.entities;

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
@Table(name = "caja_turnos")
public class CajaTurno extends BaseEntity {

    @Column(name = "numero", nullable = false, unique = true, length = 30)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caja_id", nullable = false)
    private CajaFisica caja;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "moneda", nullable = false, length = 3)
    private String moneda;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "fecha_apertura", nullable = false)
    private OffsetDateTime fechaApertura;

    @Column(name = "fecha_cierre")
    private OffsetDateTime fechaCierre;

    @Column(name = "saldo_apertura", nullable = false, precision = 18, scale = 2)
    private BigDecimal saldoApertura;

    @Column(name = "saldo_esperado", nullable = false, precision = 18, scale = 2)
    private BigDecimal saldoEsperado;

    @Column(name = "conteo_fisico", precision = 18, scale = 2)
    private BigDecimal conteoFisico;

    @Column(name = "diferencia_cierre", precision = 18, scale = 2)
    private BigDecimal diferenciaCierre;

    @Column(name = "numero_ventas", nullable = false)
    private Integer numeroVentas = 0;

    @Column(name = "total_ventas", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalVentas = BigDecimal.ZERO;

    @Column(name = "total_efectivo", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalEfectivo = BigDecimal.ZERO;

    @Column(name = "total_tarjeta", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalTarjeta = BigDecimal.ZERO;

    @Column(name = "total_billetera_digital", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalBilleteraDigital = BigDecimal.ZERO;

    @Column(name = "total_transferencia", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalTransferencia = BigDecimal.ZERO;

    @Column(name = "total_credito", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalCredito = BigDecimal.ZERO;

    @Column(name = "total_ingresos_manuales", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalIngresosManuales = BigDecimal.ZERO;

    @Column(name = "total_retiros", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalRetiros = BigDecimal.ZERO;

    @Column(name = "total_depositos", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalDepositos = BigDecimal.ZERO;

    @Column(name = "total_reembolsos", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalReembolsos = BigDecimal.ZERO;

    @Column(name = "responsable_apertura_id", nullable = false, length = 80)
    private String responsableAperturaId;

    @Column(name = "responsable_apertura_nombre", nullable = false, length = 150)
    private String responsableAperturaNombre;

    @Column(name = "responsable_cierre_id", length = 80)
    private String responsableCierreId;

    @Column(name = "responsable_cierre_nombre", length = 150)
    private String responsableCierreNombre;

    @Column(name = "observacion_apertura", length = 500)
    private String observacionApertura;

    @Column(name = "observacion_cierre", length = 500)
    private String observacionCierre;
}
