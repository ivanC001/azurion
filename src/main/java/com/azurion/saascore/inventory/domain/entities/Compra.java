package com.azurion.saascore.inventory.domain.entities;

import com.azurion.saascore.almacenes.domain.entities.Almacen;
import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "compras")
public class Compra extends BaseEntity {

    @Column(name = "proveedor_id")
    private Long proveedorId;

    @Column(name = "proveedor_documento", length = 20)
    private String proveedorDocumento;

    @Column(name = "proveedor_nombre", length = 255)
    private String proveedorNombre;

    @Column(name = "tipo_comprobante", nullable = false, length = 20)
    private String tipoComprobante;

    @Column(name = "serie", length = 20)
    private String serie;

    @Column(name = "correlativo", length = 30)
    private String correlativo;

    @Column(name = "numero_comprobante", nullable = false, length = 60)
    private String numeroComprobante;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "fecha_ingreso", nullable = false)
    private OffsetDateTime fechaIngreso = OffsetDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "almacen_id", nullable = false)
    private Almacen almacen;

    @Column(name = "total", nullable = false, precision = 18, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "subtotal_neto", nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotalNeto = BigDecimal.ZERO;

    @Column(name = "monto_igv", nullable = false, precision = 18, scale = 2)
    private BigDecimal montoIgv = BigDecimal.ZERO;

    @Column(name = "credito_fiscal_aplicable", nullable = false)
    private boolean creditoFiscalAplicable;

    @Column(name = "total_costo_inventariable", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalCostoInventariable = BigDecimal.ZERO;

    @Column(name = "tratamiento_igv", nullable = false, length = 32)
    private String tratamientoIgv = "DESGLOSADO";

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "REGISTRADA";

    @Column(name = "client_operation_id", length = 100)
    private String clientOperationId;

    @Column(name = "request_hash", length = 64)
    private String requestHash;

    @OneToMany(mappedBy = "compra")
    private List<CompraDetalle> detalles = new ArrayList<>();

    public Long getProveedorId() {
        return proveedorId;
    }

    public void setProveedorId(Long proveedorId) {
        this.proveedorId = proveedorId;
    }

    public String getProveedorDocumento() {
        return proveedorDocumento;
    }

    public void setProveedorDocumento(String proveedorDocumento) {
        this.proveedorDocumento = proveedorDocumento;
    }

    public String getProveedorNombre() {
        return proveedorNombre;
    }

    public void setProveedorNombre(String proveedorNombre) {
        this.proveedorNombre = proveedorNombre;
    }

    public String getTipoComprobante() {
        return tipoComprobante;
    }

    public void setTipoComprobante(String tipoComprobante) {
        this.tipoComprobante = tipoComprobante;
    }

    public String getSerie() {
        return serie;
    }

    public void setSerie(String serie) {
        this.serie = serie;
    }

    public String getCorrelativo() {
        return correlativo;
    }

    public void setCorrelativo(String correlativo) {
        this.correlativo = correlativo;
    }

    public String getNumeroComprobante() {
        return numeroComprobante;
    }

    public void setNumeroComprobante(String numeroComprobante) {
        this.numeroComprobante = numeroComprobante;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDate fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public OffsetDateTime getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(OffsetDateTime fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public Almacen getAlmacen() {
        return almacen;
    }

    public void setAlmacen(Almacen almacen) {
        this.almacen = almacen;
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

    public boolean isCreditoFiscalAplicable() {
        return creditoFiscalAplicable;
    }

    public void setCreditoFiscalAplicable(boolean creditoFiscalAplicable) {
        this.creditoFiscalAplicable = creditoFiscalAplicable;
    }

    public BigDecimal getTotalCostoInventariable() {
        return totalCostoInventariable;
    }

    public void setTotalCostoInventariable(BigDecimal totalCostoInventariable) {
        this.totalCostoInventariable = totalCostoInventariable;
    }

    public String getTratamientoIgv() {
        return tratamientoIgv;
    }

    public void setTratamientoIgv(String tratamientoIgv) {
        this.tratamientoIgv = tratamientoIgv;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getClientOperationId() {
        return clientOperationId;
    }

    public void setClientOperationId(String clientOperationId) {
        this.clientOperationId = clientOperationId;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public void setRequestHash(String requestHash) {
        this.requestHash = requestHash;
    }

    public List<CompraDetalle> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<CompraDetalle> detalles) {
        this.detalles = detalles;
    }
}
