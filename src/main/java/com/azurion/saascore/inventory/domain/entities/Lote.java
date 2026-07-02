package com.azurion.saascore.inventory.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "lotes", uniqueConstraints = {
        @UniqueConstraint(name = "uq_lotes_producto_codigo", columnNames = {"producto_id", "codigo_lote"})
})
public class Lote extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_detalle_id")
    private CompraDetalle compraDetalle;

    @Column(name = "codigo_lote", nullable = false, length = 120)
    private String codigoLote;

    @Column(name = "fecha_fabricacion")
    private LocalDate fechaFabricacion;

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDate fechaIngreso = LocalDate.now();

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "proveedor_id")
    private Long proveedorId;

    @Column(name = "cantidad_inicial", nullable = false, precision = 18, scale = 4)
    private BigDecimal cantidadInicial = BigDecimal.ZERO;

    @Column(name = "costo_unitario", nullable = false, precision = 18, scale = 6)
    private BigDecimal costoUnitario = BigDecimal.ZERO;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "ACTIVO";
}
