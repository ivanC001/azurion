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

    @Column(name = "crm_catalogo_item_id")
    private Long catalogoItemId;

    @Column(name = "catalogo_tipo_item", length = 30)
    private String catalogoTipoItem;

    @Column(name = "catalogo_nombre", length = 220)
    private String catalogoNombre;

    @Column(name = "catalogo_descripcion", length = 1500)
    private String catalogoDescripcion;

    @Column(name = "catalogo_metadata_json", columnDefinition = "TEXT")
    private String catalogoMetadataJson;

    @Column(name = "catalogo_moneda", length = 3)
    private String catalogoMoneda;

    @Column(name = "catalogo_precio_referencial", precision = 18, scale = 2)
    private BigDecimal catalogoPrecioReferencial;

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
