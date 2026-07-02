package com.azurion.saascore.inventory.domain.entities;

import com.azurion.saascore.almacenes.domain.entities.Almacen;
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
@Table(name = "kardex_movimientos")
public class KardexMovimiento extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "almacen_id", nullable = false)
    private Almacen almacen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id")
    private Lote lote;

    @Column(name = "tipo_movimiento", nullable = false, length = 20)
    private String tipoMovimiento;

    @Column(name = "motivo", nullable = false, length = 150)
    private String motivo;

    @Column(name = "referencia_tipo", length = 40)
    private String referenciaTipo;

    @Column(name = "referencia_id")
    private Long referenciaId;

    @Column(name = "cantidad", nullable = false, precision = 18, scale = 4)
    private BigDecimal cantidad;

    @Column(name = "stock_anterior", precision = 18, scale = 4)
    private BigDecimal stockAnterior;

    @Column(name = "stock_nuevo", precision = 18, scale = 4)
    private BigDecimal stockNuevo;

    @Column(name = "saldo_resultante", nullable = false, precision = 18, scale = 4)
    private BigDecimal saldoResultante;

    @Column(name = "costo_unitario", precision = 18, scale = 6)
    private BigDecimal costoUnitario;

    @Column(name = "costo_total", precision = 18, scale = 6)
    private BigDecimal costoTotal;

    @Column(name = "precio_compra", precision = 18, scale = 6)
    private BigDecimal precioCompra;

    @Column(name = "precio_venta", precision = 18, scale = 2)
    private BigDecimal precioVenta;

    @Column(name = "usuario_id", length = 120)
    private String usuarioId;

    @Column(name = "referencia", length = 120)
    private String referencia;

    @Column(name = "fecha_movimiento", nullable = false)
    private OffsetDateTime fechaMovimiento;

    @Column(name = "observacion", length = 500)
    private String observacion;
}
