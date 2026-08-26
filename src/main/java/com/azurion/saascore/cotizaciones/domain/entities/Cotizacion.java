package com.azurion.saascore.cotizaciones.domain.entities;

import com.azurion.saascore.clientes.domain.entities.Cliente;
import com.azurion.saascore.sucursales.domain.entities.Sucursal;
import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.CascadeType;
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
@Table(name = "cotizaciones")
public class Cotizacion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(name = "usuario_id", nullable = false, length = 80)
    private String usuarioId;

    @Column(name = "usuario_nombre", nullable = false, length = 150)
    private String usuarioNombre;

    @Column(name = "asesor_apellidos", length = 160)
    private String asesorApellidos;

    @Column(name = "asesor_telefono", length = 40)
    private String asesorTelefono;

    @Column(name = "asesor_email", length = 180)
    private String asesorEmail;

    @Column(name = "asesor_cargo", length = 120)
    private String asesorCargo;

    @Column(name = "asesor_foto_url", length = 500)
    private String asesorFotoUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "moneda", nullable = false, length = 3)
    private String moneda = "PEN";

    @Column(name = "moneda_base", nullable = false, length = 3)
    private String monedaBase = "PEN";

    @Column(name = "tipo_cambio_aplicado", nullable = false, precision = 18, scale = 6)
    private BigDecimal tipoCambioAplicado = BigDecimal.ONE;

    @Column(name = "fecha_tipo_cambio", nullable = false)
    private OffsetDateTime fechaTipoCambio = OffsetDateTime.now();

    @Column(name = "subtotal", nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 18, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "subtotal_moneda_base", nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotalMonedaBase = BigDecimal.ZERO;

    @Column(name = "total_moneda_base", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalMonedaBase = BigDecimal.ZERO;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "BORRADOR";

    @Column(name = "observacion", length = 500)
    private String observacion;

    @Column(name = "venta_id")
    private Long ventaId;

    @Column(name = "crm_oportunidad_id")
    private Long crmOportunidadId;

    @Column(name = "fecha_envio")
    private OffsetDateTime fechaEnvio;

    @Column(name = "canal_envio", length = 30)
    private String canalEnvio;

    @Column(name = "proximo_seguimiento_en")
    private OffsetDateTime proximoSeguimientoEn;

    @Column(name = "fecha_respuesta")
    private OffsetDateTime fechaRespuesta;

    @Column(name = "motivo_rechazo", length = 500)
    private String motivoRechazo;

    @Column(name = "decision_siguiente", length = 30)
    private String decisionSiguiente;

    @Column(name = "email_send_status", length = 20)
    private String emailSendStatus;

    @Column(name = "email_send_token", length = 80)
    private String emailSendToken;

    @Column(name = "email_send_started_at")
    private OffsetDateTime emailSendStartedAt;

    @Column(name = "email_send_error", length = 500)
    private String emailSendError;

    @Column(name = "whatsapp_send_status", length = 20)
    private String whatsappSendStatus;

    @Column(name = "whatsapp_send_token", length = 80)
    private String whatsappSendToken;

    @Column(name = "whatsapp_send_started_at")
    private OffsetDateTime whatsappSendStartedAt;

    @Column(name = "whatsapp_send_error", length = 500)
    private String whatsappSendError;

    @Column(name = "whatsapp_message_id", length = 255)
    private String whatsappMessageId;

    @Column(name = "convertida_en")
    private OffsetDateTime convertidaEn;

    @OneToMany(mappedBy = "cotizacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CotizacionDetalle> detalles = new ArrayList<>();

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
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

    public String getAsesorApellidos() {
        return asesorApellidos;
    }

    public void setAsesorApellidos(String asesorApellidos) {
        this.asesorApellidos = asesorApellidos;
    }

    public String getAsesorTelefono() {
        return asesorTelefono;
    }

    public void setAsesorTelefono(String asesorTelefono) {
        this.asesorTelefono = asesorTelefono;
    }

    public String getAsesorEmail() {
        return asesorEmail;
    }

    public void setAsesorEmail(String asesorEmail) {
        this.asesorEmail = asesorEmail;
    }

    public String getAsesorCargo() {
        return asesorCargo;
    }

    public void setAsesorCargo(String asesorCargo) {
        this.asesorCargo = asesorCargo;
    }

    public String getAsesorFotoUrl() {
        return asesorFotoUrl;
    }

    public void setAsesorFotoUrl(String asesorFotoUrl) {
        this.asesorFotoUrl = asesorFotoUrl;
    }

    public Sucursal getSucursal() {
        return sucursal;
    }

    public void setSucursal(Sucursal sucursal) {
        this.sucursal = sucursal;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDate fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public String getMonedaBase() {
        return monedaBase;
    }

    public void setMonedaBase(String monedaBase) {
        this.monedaBase = monedaBase;
    }

    public BigDecimal getTipoCambioAplicado() {
        return tipoCambioAplicado;
    }

    public void setTipoCambioAplicado(BigDecimal tipoCambioAplicado) {
        this.tipoCambioAplicado = tipoCambioAplicado;
    }

    public OffsetDateTime getFechaTipoCambio() {
        return fechaTipoCambio;
    }

    public void setFechaTipoCambio(OffsetDateTime fechaTipoCambio) {
        this.fechaTipoCambio = fechaTipoCambio;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getSubtotalMonedaBase() {
        return subtotalMonedaBase;
    }

    public void setSubtotalMonedaBase(BigDecimal subtotalMonedaBase) {
        this.subtotalMonedaBase = subtotalMonedaBase;
    }

    public BigDecimal getTotalMonedaBase() {
        return totalMonedaBase;
    }

    public void setTotalMonedaBase(BigDecimal totalMonedaBase) {
        this.totalMonedaBase = totalMonedaBase;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public Long getVentaId() {
        return ventaId;
    }

    public void setVentaId(Long ventaId) {
        this.ventaId = ventaId;
    }

    public Long getCrmOportunidadId() {
        return crmOportunidadId;
    }

    public void setCrmOportunidadId(Long crmOportunidadId) {
        this.crmOportunidadId = crmOportunidadId;
    }

    public OffsetDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(OffsetDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public String getCanalEnvio() {
        return canalEnvio;
    }

    public void setCanalEnvio(String canalEnvio) {
        this.canalEnvio = canalEnvio;
    }

    public OffsetDateTime getProximoSeguimientoEn() {
        return proximoSeguimientoEn;
    }

    public void setProximoSeguimientoEn(OffsetDateTime proximoSeguimientoEn) {
        this.proximoSeguimientoEn = proximoSeguimientoEn;
    }

    public OffsetDateTime getFechaRespuesta() {
        return fechaRespuesta;
    }

    public void setFechaRespuesta(OffsetDateTime fechaRespuesta) {
        this.fechaRespuesta = fechaRespuesta;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public String getDecisionSiguiente() {
        return decisionSiguiente;
    }

    public void setDecisionSiguiente(String decisionSiguiente) {
        this.decisionSiguiente = decisionSiguiente;
    }

    public String getEmailSendStatus() {
        return emailSendStatus;
    }

    public void setEmailSendStatus(String emailSendStatus) {
        this.emailSendStatus = emailSendStatus;
    }

    public String getEmailSendToken() {
        return emailSendToken;
    }

    public void setEmailSendToken(String emailSendToken) {
        this.emailSendToken = emailSendToken;
    }

    public OffsetDateTime getEmailSendStartedAt() {
        return emailSendStartedAt;
    }

    public void setEmailSendStartedAt(OffsetDateTime emailSendStartedAt) {
        this.emailSendStartedAt = emailSendStartedAt;
    }

    public String getEmailSendError() {
        return emailSendError;
    }

    public void setEmailSendError(String emailSendError) {
        this.emailSendError = emailSendError;
    }

    public String getWhatsappSendStatus() {
        return whatsappSendStatus;
    }

    public void setWhatsappSendStatus(String whatsappSendStatus) {
        this.whatsappSendStatus = whatsappSendStatus;
    }

    public String getWhatsappSendToken() {
        return whatsappSendToken;
    }

    public void setWhatsappSendToken(String whatsappSendToken) {
        this.whatsappSendToken = whatsappSendToken;
    }

    public OffsetDateTime getWhatsappSendStartedAt() {
        return whatsappSendStartedAt;
    }

    public void setWhatsappSendStartedAt(OffsetDateTime whatsappSendStartedAt) {
        this.whatsappSendStartedAt = whatsappSendStartedAt;
    }

    public String getWhatsappSendError() {
        return whatsappSendError;
    }

    public void setWhatsappSendError(String whatsappSendError) {
        this.whatsappSendError = whatsappSendError;
    }

    public String getWhatsappMessageId() {
        return whatsappMessageId;
    }

    public void setWhatsappMessageId(String whatsappMessageId) {
        this.whatsappMessageId = whatsappMessageId;
    }

    public OffsetDateTime getConvertidaEn() {
        return convertidaEn;
    }

    public void setConvertidaEn(OffsetDateTime convertidaEn) {
        this.convertidaEn = convertidaEn;
    }

    public List<CotizacionDetalle> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<CotizacionDetalle> detalles) {
        this.detalles = detalles;
    }
}
