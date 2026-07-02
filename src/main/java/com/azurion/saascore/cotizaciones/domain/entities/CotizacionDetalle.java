package com.azurion.saascore.cotizaciones.domain.entities;

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
@Table(name = "cotizacion_detalles")
public class CotizacionDetalle extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cotizacion_id", nullable = false)
    private Cotizacion cotizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promocion_id")
    private PromocionCotizacion promocion;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "cantidad", nullable = false, precision = 18, scale = 4)
    private BigDecimal cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 18, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "descuento", nullable = false, precision = 18, scale = 2)
    private BigDecimal descuento = BigDecimal.ZERO;

    @Column(name = "promocion_descuento", nullable = false, precision = 18, scale = 2)
    private BigDecimal promocionDescuento = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 18, scale = 2)
    private BigDecimal total;
}
