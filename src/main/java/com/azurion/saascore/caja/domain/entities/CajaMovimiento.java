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
@Table(name = "caja_movimientos")
public class CajaMovimiento extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "turno_id", nullable = false)
    private CajaTurno turno;

    @Column(name = "tipo_movimiento", nullable = false, length = 30)
    private String tipoMovimiento;

    @Column(name = "monto", nullable = false, precision = 18, scale = 2)
    private BigDecimal monto;

    @Column(name = "saldo_anterior", nullable = false, precision = 18, scale = 2)
    private BigDecimal saldoAnterior;

    @Column(name = "saldo_resultante", nullable = false, precision = 18, scale = 2)
    private BigDecimal saldoResultante;

    @Column(name = "descripcion", nullable = false, length = 250)
    private String descripcion;

    @Column(name = "referencia", length = 120)
    private String referencia;

    @Column(name = "cuenta_empresarial", length = 120)
    private String cuentaEmpresarial;

    @Column(name = "origen", nullable = false, length = 30)
    private String origen;

    @Column(name = "medio_pago", nullable = false, length = 30)
    private String medioPago;

    @Column(name = "afecta_efectivo", nullable = false)
    private boolean afectaEfectivo;

    @Column(name = "venta_id")
    private Long ventaId;

    @Column(name = "anulado", nullable = false)
    private boolean anulado;

    @Column(name = "motivo_anulacion", length = 500)
    private String motivoAnulacion;

    @Column(name = "responsable_id", nullable = false, length = 80)
    private String responsableId;

    @Column(name = "responsable_nombre", nullable = false, length = 150)
    private String responsableNombre;

    @Column(name = "fecha_movimiento", nullable = false)
    private OffsetDateTime fechaMovimiento;

    @Column(name = "client_operation_id", length = 100)
    private String clientOperationId;

    @Column(name = "request_hash", length = 64)
    private String requestHash;
}
