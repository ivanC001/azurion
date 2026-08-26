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

    @Column(name = "costo_unitario_inventariable", precision = 18, scale = 6)
    private BigDecimal costoUnitarioInventariable;

    @Column(name = "total", nullable = false, precision = 18, scale = 2)
    private BigDecimal total;

    public Venta getVenta() {
        return venta;
    }

    public void setVenta(Venta venta) {
        this.venta = venta;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public BigDecimal getDescuento() {
        return descuento;
    }

    public void setDescuento(BigDecimal descuento) {
        this.descuento = descuento;
    }

    public String getTipoOperacionCodigo() {
        return tipoOperacionCodigo;
    }

    public void setTipoOperacionCodigo(String tipoOperacionCodigo) {
        this.tipoOperacionCodigo = tipoOperacionCodigo;
    }

    public String getTipoAfectacionIgvCodigo() {
        return tipoAfectacionIgvCodigo;
    }

    public void setTipoAfectacionIgvCodigo(String tipoAfectacionIgvCodigo) {
        this.tipoAfectacionIgvCodigo = tipoAfectacionIgvCodigo;
    }

    public String getTributoCodigo() {
        return tributoCodigo;
    }

    public void setTributoCodigo(String tributoCodigo) {
        this.tributoCodigo = tributoCodigo;
    }

    public BigDecimal getPorcentajeIgv() {
        return porcentajeIgv;
    }

    public void setPorcentajeIgv(BigDecimal porcentajeIgv) {
        this.porcentajeIgv = porcentajeIgv;
    }

    public BigDecimal getBaseImponible() {
        return baseImponible;
    }

    public void setBaseImponible(BigDecimal baseImponible) {
        this.baseImponible = baseImponible;
    }

    public BigDecimal getMontoIgv() {
        return montoIgv;
    }

    public void setMontoIgv(BigDecimal montoIgv) {
        this.montoIgv = montoIgv;
    }

    public BigDecimal getCostoUnitarioInventariable() {
        return costoUnitarioInventariable;
    }

    public void setCostoUnitarioInventariable(BigDecimal costoUnitarioInventariable) {
        this.costoUnitarioInventariable = costoUnitarioInventariable;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }
}
