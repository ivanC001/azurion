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
}
