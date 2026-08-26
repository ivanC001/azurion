package com.azurion.saascore.inventory.domain.entities;

import com.azurion.saascore.almacenes.domain.entities.Almacen;
import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "stock", uniqueConstraints = {
        @UniqueConstraint(name = "uq_stock_producto_almacen", columnNames = {"producto_id", "almacen_id"})
})
public class Stock extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "almacen_id", nullable = false)
    private Almacen almacen;

    @Column(name = "cantidad", nullable = false, precision = 18, scale = 4)
    private BigDecimal cantidad;

    @Column(name = "stock_reservado", nullable = false, precision = 18, scale = 4)
    private BigDecimal stockReservado = BigDecimal.ZERO;

    @Column(name = "stock_minimo", nullable = false, precision = 18, scale = 4)
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    @Column(name = "stock_maximo", precision = 18, scale = 4)
    private BigDecimal stockMaximo;

    @Column(name = "ubicacion_fisica", length = 120)
    private String ubicacionFisica;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "ACTIVO";

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public Almacen getAlmacen() {
        return almacen;
    }

    public void setAlmacen(Almacen almacen) {
        this.almacen = almacen;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getStockReservado() {
        return stockReservado;
    }

    public void setStockReservado(BigDecimal stockReservado) {
        this.stockReservado = stockReservado;
    }

    public BigDecimal getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(BigDecimal stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public BigDecimal getStockMaximo() {
        return stockMaximo;
    }

    public void setStockMaximo(BigDecimal stockMaximo) {
        this.stockMaximo = stockMaximo;
    }

    public String getUbicacionFisica() {
        return ubicacionFisica;
    }

    public void setUbicacionFisica(String ubicacionFisica) {
        this.ubicacionFisica = ubicacionFisica;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
