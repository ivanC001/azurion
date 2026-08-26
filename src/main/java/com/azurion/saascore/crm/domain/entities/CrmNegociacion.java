package com.azurion.saascore.crm.domain.entities;

import com.azurion.saascore.cotizaciones.domain.entities.Cotizacion;
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
@Table(name = "crm_negociaciones")
public class CrmNegociacion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "oportunidad_id", nullable = false)
    private CrmOportunidad oportunidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cotizacion_id")
    private Cotizacion cotizacion;

    @Column(nullable = false, length = 40)
    private String estado = "AJUSTE_SOLICITADO";

    @Column(name = "solicitud_cliente", nullable = false, length = 80)
    private String solicitudCliente = "MEJOR_PRECIO";

    @Column(name = "precio_original", nullable = false, precision = 18, scale = 2)
    private BigDecimal precioOriginal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal descuento = BigDecimal.ZERO;

    @Column(name = "precio_final", nullable = false, precision = 18, scale = 2)
    private BigDecimal precioFinal = BigDecimal.ZERO;

    @Column(name = "forma_pago", length = 80)
    private String formaPago;

    @Column(nullable = false)
    private Integer cuotas = 1;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_entrega")
    private LocalDate fechaEntrega;

    @Column(columnDefinition = "TEXT")
    private String observacion;

    @Column(nullable = false, length = 40)
    private String resultado = "PENDIENTE";

    @Column(name = "usuario_id", length = 120)
    private String usuarioId;

    @Column(name = "usuario_nombre", length = 160)
    private String usuarioNombre;

    public CrmOportunidad getOportunidad() {
        return oportunidad;
    }

    public void setOportunidad(CrmOportunidad oportunidad) {
        this.oportunidad = oportunidad;
    }

    public Cotizacion getCotizacion() {
        return cotizacion;
    }

    public void setCotizacion(Cotizacion cotizacion) {
        this.cotizacion = cotizacion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getSolicitudCliente() {
        return solicitudCliente;
    }

    public void setSolicitudCliente(String solicitudCliente) {
        this.solicitudCliente = solicitudCliente;
    }

    public BigDecimal getPrecioOriginal() {
        return precioOriginal;
    }

    public void setPrecioOriginal(BigDecimal precioOriginal) {
        this.precioOriginal = precioOriginal;
    }

    public BigDecimal getDescuento() {
        return descuento;
    }

    public void setDescuento(BigDecimal descuento) {
        this.descuento = descuento;
    }

    public BigDecimal getPrecioFinal() {
        return precioFinal;
    }

    public void setPrecioFinal(BigDecimal precioFinal) {
        this.precioFinal = precioFinal;
    }

    public String getFormaPago() {
        return formaPago;
    }

    public void setFormaPago(String formaPago) {
        this.formaPago = formaPago;
    }

    public Integer getCuotas() {
        return cuotas;
    }

    public void setCuotas(Integer cuotas) {
        this.cuotas = cuotas;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(LocalDate fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getUsuarioNombre() {
        return usuarioNombre;
    }

    public void setUsuarioNombre(String usuarioNombre) {
        this.usuarioNombre = usuarioNombre;
    }
}
