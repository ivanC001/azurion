package com.azurion.saascore.ventas.domain.entities;

import com.azurion.saascore.inventory.domain.entities.Producto;
import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "venta_detalles")
public class VentaDetalle extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Column(name = "sku", nullable = false, length = 80)
    private String sku;

    @Column(name = "descripcion", nullable = false, length = 255)
    private String descripcion;

    @Column(name = "cantidad", nullable = false, precision = 18, scale = 4)
    private BigDecimal cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 18, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "descuento", nullable = false, precision = 18, scale = 2)
    private BigDecimal descuento;

    @Column(name = "tipo_operacion_codigo", nullable = false, length = 4)
    private String tipoOperacionCodigo;

    @Column(name = "tipo_afectacion_igv_codigo", nullable = false, length = 4)
    private String tipoAfectacionIgvCodigo;

    @Column(name = "tributo_codigo", nullable = false, length = 6)
    private String tributoCodigo;

    @Column(name = "porcentaje_igv", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeIgv;

    @Column(name = "base_imponible", nullable = false, precision = 18, scale = 2)
    private BigDecimal baseImponible;

    @Column(name = "monto_igv", nullable = false, precision = 18, scale = 2)
    private BigDecimal montoIgv;

    @Column(name = "total", nullable = false, precision = 18, scale = 2)
    private BigDecimal total;
}
