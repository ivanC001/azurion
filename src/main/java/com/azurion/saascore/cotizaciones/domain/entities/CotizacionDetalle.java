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

    public Cotizacion getCotizacion() {
        return cotizacion;
    }

    public void setCotizacion(Cotizacion cotizacion) {
        this.cotizacion = cotizacion;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public PromocionCotizacion getPromocion() {
        return promocion;
    }

    public void setPromocion(PromocionCotizacion promocion) {
        this.promocion = promocion;
    }

    public Long getCatalogoItemId() {
        return catalogoItemId;
    }

    public void setCatalogoItemId(Long catalogoItemId) {
        this.catalogoItemId = catalogoItemId;
    }

    public String getCatalogoTipoItem() {
        return catalogoTipoItem;
    }

    public void setCatalogoTipoItem(String catalogoTipoItem) {
        this.catalogoTipoItem = catalogoTipoItem;
    }

    public String getCatalogoNombre() {
        return catalogoNombre;
    }

    public void setCatalogoNombre(String catalogoNombre) {
        this.catalogoNombre = catalogoNombre;
    }

    public String getCatalogoDescripcion() {
        return catalogoDescripcion;
    }

    public void setCatalogoDescripcion(String catalogoDescripcion) {
        this.catalogoDescripcion = catalogoDescripcion;
    }

    public String getCatalogoMetadataJson() {
        return catalogoMetadataJson;
    }

    public void setCatalogoMetadataJson(String catalogoMetadataJson) {
        this.catalogoMetadataJson = catalogoMetadataJson;
    }

    public String getCatalogoMoneda() {
        return catalogoMoneda;
    }

    public void setCatalogoMoneda(String catalogoMoneda) {
        this.catalogoMoneda = catalogoMoneda;
    }

    public BigDecimal getCatalogoPrecioReferencial() {
        return catalogoPrecioReferencial;
    }

    public void setCatalogoPrecioReferencial(BigDecimal catalogoPrecioReferencial) {
        this.catalogoPrecioReferencial = catalogoPrecioReferencial;
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

    public BigDecimal getPromocionDescuento() {
        return promocionDescuento;
    }

    public void setPromocionDescuento(BigDecimal promocionDescuento) {
        this.promocionDescuento = promocionDescuento;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }
}
