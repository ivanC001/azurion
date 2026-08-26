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

    public Compra getCompra() {
        return compra;
    }

    public void setCompra(Compra compra) {
        this.compra = compra;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public void setCostoUnitario(BigDecimal costoUnitario) {
        this.costoUnitario = costoUnitario;
    }

    public BigDecimal getCostoNetoUnitario() {
        return costoNetoUnitario;
    }

    public void setCostoNetoUnitario(BigDecimal costoNetoUnitario) {
        this.costoNetoUnitario = costoNetoUnitario;
    }

    public BigDecimal getPorcentajeIgv() {
        return porcentajeIgv;
    }

    public void setPorcentajeIgv(BigDecimal porcentajeIgv) {
        this.porcentajeIgv = porcentajeIgv;
    }

    public BigDecimal getMontoIgvUnitario() {
        return montoIgvUnitario;
    }

    public void setMontoIgvUnitario(BigDecimal montoIgvUnitario) {
        this.montoIgvUnitario = montoIgvUnitario;
    }

    public BigDecimal getCostoTotalUnitario() {
        return costoTotalUnitario;
    }

    public void setCostoTotalUnitario(BigDecimal costoTotalUnitario) {
        this.costoTotalUnitario = costoTotalUnitario;
    }

    public BigDecimal getCostoInventariableUnitario() {
        return costoInventariableUnitario;
    }

    public void setCostoInventariableUnitario(BigDecimal costoInventariableUnitario) {
        this.costoInventariableUnitario = costoInventariableUnitario;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(BigDecimal precioVenta) {
        this.precioVenta = precioVenta;
    }

    public BigDecimal getPrecioVentaNeto() {
        return precioVentaNeto;
    }

    public void setPrecioVentaNeto(BigDecimal precioVentaNeto) {
        this.precioVentaNeto = precioVentaNeto;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getSubtotalNeto() {
        return subtotalNeto;
    }

    public void setSubtotalNeto(BigDecimal subtotalNeto) {
        this.subtotalNeto = subtotalNeto;
    }

    public BigDecimal getMontoIgv() {
        return montoIgv;
    }

    public void setMontoIgv(BigDecimal montoIgv) {
        this.montoIgv = montoIgv;
    }

    public BigDecimal getTotalCostoInventariable() {
        return totalCostoInventariable;
    }

    public void setTotalCostoInventariable(BigDecimal totalCostoInventariable) {
        this.totalCostoInventariable = totalCostoInventariable;
    }

    public String getCodigoLote() {
        return codigoLote;
    }

    public void setCodigoLote(String codigoLote) {
        this.codigoLote = codigoLote;
    }

    public LocalDate getFechaFabricacion() {
        return fechaFabricacion;
    }

    public void setFechaFabricacion(LocalDate fechaFabricacion) {
        this.fechaFabricacion = fechaFabricacion;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }
}
