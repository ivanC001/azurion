package com.azurion.saascore.inventory.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "compra_detalles")
public class CompraDetalle extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compra_id", nullable = false)
    private Compra compra;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "cantidad", nullable = false, precision = 18, scale = 4)
    private BigDecimal cantidad;

    @Column(name = "costo_unitario", nullable = false, precision = 18, scale = 6)
    private BigDecimal costoUnitario;

    @Column(name = "costo_neto_unitario", precision = 18, scale = 6)
    private BigDecimal costoNetoUnitario;

    @Column(name = "porcentaje_igv", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeIgv = BigDecimal.ZERO;

    @Column(name = "monto_igv_unitario", nullable = false, precision = 18, scale = 6)
    private BigDecimal montoIgvUnitario = BigDecimal.ZERO;

    @Column(name = "costo_total_unitario", precision = 18, scale = 6)
    private BigDecimal costoTotalUnitario;

    @Column(name = "costo_inventariable_unitario", precision = 18, scale = 6)
    private BigDecimal costoInventariableUnitario;

    @Column(name = "precio_venta", precision = 18, scale = 2)
    private BigDecimal precioVenta;

    @Column(name = "precio_venta_neto", precision = 18, scale = 6)
    private BigDecimal precioVentaNeto;

    @Column(name = "total", nullable = false, precision = 18, scale = 2)
    private BigDecimal total;

    @Column(name = "subtotal_neto", nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotalNeto = BigDecimal.ZERO;

    @Column(name = "monto_igv", nullable = false, precision = 18, scale = 2)
    private BigDecimal montoIgv = BigDecimal.ZERO;

    @Column(name = "total_costo_inventariable", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalCostoInventariable = BigDecimal.ZERO;

    @Column(name = "codigo_lote", length = 120)
    private String codigoLote;

    @Column(name = "fecha_fabricacion")
    private LocalDate fechaFabricacion;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;
}
