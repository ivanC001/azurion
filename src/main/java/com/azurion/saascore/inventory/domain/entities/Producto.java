package com.azurion.saascore.inventory.domain.entities;

import com.azurion.saascore.almacenes.domain.entities.Almacen;
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
@Table(name = "productos")
public class Producto extends BaseEntity {

    public static final String PRECIO_VENTA_MODO_INCLUYE_IGV = "INCLUYE_IGV";

    @Column(name = "sku", nullable = false, unique = true, length = 80)
    private String sku;

    @Column(name = "codigo", length = 80)
    private String codigo;

    @Column(name = "codigo_barras", length = 80)
    private String codigoBarras;

    @Column(name = "nombre", nullable = false, length = 255)
    private String nombre;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "precio", nullable = false, precision = 18, scale = 2)
    private BigDecimal precio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marca_id")
    private Marca marca;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidad_medida_id")
    private UnidadMedida unidadMedida;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "almacen_id")
    private Almacen almacen;

    @Column(name = "tipo_producto", nullable = false, length = 30)
    private String tipoProducto = "PRODUCTO";

    @Column(name = "imagen_url", columnDefinition = "TEXT")
    private String imagenUrl;

    @Column(name = "precio_compra_base", nullable = false, precision = 18, scale = 2)
    private BigDecimal precioCompraBase = BigDecimal.ZERO;

    @Column(name = "precio_venta_base", precision = 18, scale = 2)
    private BigDecimal precioVentaBase;

    @Column(name = "precio_venta_modo", nullable = false, length = 24)
    private String precioVentaModo = PRECIO_VENTA_MODO_INCLUYE_IGV;

    @Column(name = "costo_promedio", nullable = false, precision = 18, scale = 6)
    private BigDecimal costoPromedio = BigDecimal.ZERO;

    @Column(name = "afecto_igv", nullable = false)
    private boolean afectoIgv = true;

    @Column(name = "tipo_afectacion_igv_id", length = 4)
    private String tipoAfectacionIgvId;

    @Column(name = "tributo_id", length = 6)
    private String tributoId;

    @Column(name = "porcentaje_impuesto", precision = 5, scale = 2)
    private BigDecimal porcentajeImpuesto;

    @Column(name = "usa_configuracion_empresa", nullable = false)
    private boolean usaConfiguracionEmpresa = true;

    @Column(name = "maneja_stock", nullable = false)
    private boolean manejaStock = true;

    @Column(name = "maneja_lotes", nullable = false)
    private boolean manejaLotes = false;

    @Column(name = "maneja_vencimiento", nullable = false)
    private boolean manejaVencimiento = false;

    @Column(name = "stock_minimo_global", nullable = false, precision = 18, scale = 4)
    private BigDecimal stockMinimoGlobal = BigDecimal.ZERO;

    @Column(name = "stock_minimo", nullable = false, precision = 18, scale = 4)
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    @Column(name = "foto", columnDefinition = "TEXT")
    private String foto;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "ACTIVO";

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public void setCodigoBarras(String codigoBarras) {
        this.codigoBarras = codigoBarras;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public Marca getMarca() {
        return marca;
    }

    public void setMarca(Marca marca) {
        this.marca = marca;
    }

    public UnidadMedida getUnidadMedida() {
        return unidadMedida;
    }

    public void setUnidadMedida(UnidadMedida unidadMedida) {
        this.unidadMedida = unidadMedida;
    }

    public Almacen getAlmacen() {
        return almacen;
    }

    public void setAlmacen(Almacen almacen) {
        this.almacen = almacen;
    }

    public String getTipoProducto() {
        return tipoProducto;
    }

    public void setTipoProducto(String tipoProducto) {
        this.tipoProducto = tipoProducto;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public BigDecimal getPrecioCompraBase() {
        return precioCompraBase;
    }

    public void setPrecioCompraBase(BigDecimal precioCompraBase) {
        this.precioCompraBase = precioCompraBase;
    }

    public BigDecimal getPrecioVentaBase() {
        return precioVentaBase;
    }

    public void setPrecioVentaBase(BigDecimal precioVentaBase) {
        this.precioVentaBase = precioVentaBase;
    }

    public String getPrecioVentaModo() {
        return precioVentaModo;
    }

    public void setPrecioVentaModo(String precioVentaModo) {
        this.precioVentaModo = precioVentaModo;
    }

    public BigDecimal getCostoPromedio() {
        return costoPromedio;
    }

    public void setCostoPromedio(BigDecimal costoPromedio) {
        this.costoPromedio = costoPromedio;
    }

    public boolean isAfectoIgv() {
        return afectoIgv;
    }

    public void setAfectoIgv(boolean afectoIgv) {
        this.afectoIgv = afectoIgv;
    }

    public String getTipoAfectacionIgvId() {
        return tipoAfectacionIgvId;
    }

    public void setTipoAfectacionIgvId(String tipoAfectacionIgvId) {
        this.tipoAfectacionIgvId = tipoAfectacionIgvId;
    }

    public String getTributoId() {
        return tributoId;
    }

    public void setTributoId(String tributoId) {
        this.tributoId = tributoId;
    }

    public BigDecimal getPorcentajeImpuesto() {
        return porcentajeImpuesto;
    }

    public void setPorcentajeImpuesto(BigDecimal porcentajeImpuesto) {
        this.porcentajeImpuesto = porcentajeImpuesto;
    }

    public boolean isUsaConfiguracionEmpresa() {
        return usaConfiguracionEmpresa;
    }

    public void setUsaConfiguracionEmpresa(boolean usaConfiguracionEmpresa) {
        this.usaConfiguracionEmpresa = usaConfiguracionEmpresa;
    }

    public boolean isManejaStock() {
        return manejaStock;
    }

    public void setManejaStock(boolean manejaStock) {
        this.manejaStock = manejaStock;
    }

    public boolean isManejaLotes() {
        return manejaLotes;
    }

    public void setManejaLotes(boolean manejaLotes) {
        this.manejaLotes = manejaLotes;
    }

    public boolean isManejaVencimiento() {
        return manejaVencimiento;
    }

    public void setManejaVencimiento(boolean manejaVencimiento) {
        this.manejaVencimiento = manejaVencimiento;
    }

    public BigDecimal getStockMinimoGlobal() {
        return stockMinimoGlobal;
    }

    public void setStockMinimoGlobal(BigDecimal stockMinimoGlobal) {
        this.stockMinimoGlobal = stockMinimoGlobal;
    }

    public BigDecimal getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(BigDecimal stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
