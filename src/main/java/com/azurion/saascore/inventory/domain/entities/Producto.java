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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "almacen_id", nullable = false)
    private Almacen almacen;

    @Column(name = "tipo_producto", nullable = false, length = 30)
    private String tipoProducto = "PRODUCTO";

    @Column(name = "imagen_url", columnDefinition = "TEXT")
    private String imagenUrl;

    @Column(name = "precio_compra_base", nullable = false, precision = 18, scale = 2)
    private BigDecimal precioCompraBase = BigDecimal.ZERO;

    @Column(name = "precio_venta_base", precision = 18, scale = 2)
    private BigDecimal precioVentaBase;

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
}
