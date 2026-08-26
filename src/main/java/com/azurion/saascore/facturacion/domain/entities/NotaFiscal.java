package com.azurion.saascore.facturacion.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "notas_fiscales")
public class NotaFiscal extends BaseEntity {

    public static final String TIPO_DOCUMENTO_CREDITO = "07";
    public static final String TIPO_DOCUMENTO_DEBITO = "08";
    public static final String TIPO_NOTA_CREDITO = "CREDITO";
    public static final String TIPO_NOTA_DEBITO = "DEBITO";

    public static final String ESTADO_PENDIENTE = "PENDIENTE";
    public static final String ESTADO_PROCESANDO = "PROCESANDO";
    public static final String ESTADO_ACEPTADO = "ACEPTADO";
    public static final String ESTADO_RECHAZADO = "RECHAZADO";
    public static final String ESTADO_ERROR = "ERROR";

    @Column(name = "external_id", nullable = false, unique = true, length = 80)
    private String externalId;

    @Column(name = "client_operation_id", length = 100)
    private String clientOperationId;

    @Column(name = "request_hash", length = 64)
    private String requestHash;

    @Column(name = "tipo_documento", nullable = false, length = 2)
    private String tipoDocumento;

    @Column(name = "tipo_nota", nullable = false, length = 20)
    private String tipoNota;

    @Column(name = "venta_id", nullable = false)
    private Long ventaId;

    @Column(name = "venta_external_id", nullable = false, length = 80)
    private String ventaExternalId;

    @Column(name = "venta_tipo_documento", length = 3)
    private String ventaTipoDocumento;

    @Column(name = "venta_numero_documento", length = 40)
    private String ventaNumeroDocumento;

    @Column(name = "cliente_documento", nullable = false, length = 20)
    private String clienteDocumento;

    @Column(name = "cliente_nombre", nullable = false, length = 255)
    private String clienteNombre;

    @Column(name = "moneda", nullable = false, length = 3)
    private String moneda;

    @Column(name = "monto", nullable = false, precision = 18, scale = 2)
    private BigDecimal monto;

    @Column(name = "base_imponible", precision = 18, scale = 2)
    private BigDecimal baseImponible;

    @Column(name = "monto_igv", precision = 18, scale = 2)
    private BigDecimal montoIgv;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "motivo_codigo", nullable = false, length = 6)
    private String motivoCodigo;

    @Column(name = "motivo_descripcion", nullable = false, length = 255)
    private String motivoDescripcion;

    @Column(name = "responsable_id", nullable = false, length = 120)
    private String responsableId;

    @Column(name = "responsable_nombre", nullable = false, length = 255)
    private String responsableNombre;

    @Column(name = "facturacion_estado", nullable = false, length = 20)
    private String facturacionEstado;

    @Column(name = "facturacion_intentos", nullable = false)
    private Integer facturacionIntentos;

    @Column(name = "facturador_http_status")
    private Integer facturadorHttpStatus;

    @Column(name = "facturador_endpoint", length = 120)
    private String facturadorEndpoint;

    @Column(name = "facturador_tipo_comprobante", length = 30)
    private String facturadorTipoComprobante;

    @Column(name = "facturador_mensaje", length = 500)
    private String facturadorMensaje;

    @Column(name = "facturador_sunat_estado", length = 30)
    private String facturadorSunatEstado;

    @Column(name = "facturador_documento_id", length = 80)
    private String facturadorDocumentoId;

    @Column(name = "facturador_ticket", length = 120)
    private String facturadorTicket;

    @Column(name = "facturador_pdf_url", length = 500)
    private String facturadorPdfUrl;

    @Column(name = "facturador_xml_url", length = 500)
    private String facturadorXmlUrl;

    @Column(name = "facturador_cdr_url", length = 500)
    private String facturadorCdrUrl;

    @Column(name = "facturador_respuesta_json", columnDefinition = "TEXT")
    private String facturadorRespuestaJson;

    @Column(name = "facturacion_actualizado_en")
    private OffsetDateTime facturacionActualizadoEn;

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
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

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getTipoNota() {
        return tipoNota;
    }

    public void setTipoNota(String tipoNota) {
        this.tipoNota = tipoNota;
    }

    public Long getVentaId() {
        return ventaId;
    }

    public void setVentaId(Long ventaId) {
        this.ventaId = ventaId;
    }

    public String getVentaExternalId() {
        return ventaExternalId;
    }

    public void setVentaExternalId(String ventaExternalId) {
        this.ventaExternalId = ventaExternalId;
    }

    public String getVentaTipoDocumento() {
        return ventaTipoDocumento;
    }

    public void setVentaTipoDocumento(String ventaTipoDocumento) {
        this.ventaTipoDocumento = ventaTipoDocumento;
    }

    public String getVentaNumeroDocumento() {
        return ventaNumeroDocumento;
    }

    public void setVentaNumeroDocumento(String ventaNumeroDocumento) {
        this.ventaNumeroDocumento = ventaNumeroDocumento;
    }

    public String getClienteDocumento() {
        return clienteDocumento;
    }

    public void setClienteDocumento(String clienteDocumento) {
        this.clienteDocumento = clienteDocumento;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public BigDecimal getBaseImponible() {
        return baseImponible;
    }

    public void setBaseImponible(BigDecimal baseImponible) {
        this.baseImponible = baseImponible;
    }

    public BigDecimal getMontoIgv() {
        return montoIgv;
    }

    public void setMontoIgv(BigDecimal montoIgv) {
        this.montoIgv = montoIgv;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDate fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public String getMotivoCodigo() {
        return motivoCodigo;
    }

    public void setMotivoCodigo(String motivoCodigo) {
        this.motivoCodigo = motivoCodigo;
    }

    public String getMotivoDescripcion() {
        return motivoDescripcion;
    }

    public void setMotivoDescripcion(String motivoDescripcion) {
        this.motivoDescripcion = motivoDescripcion;
    }

    public String getResponsableId() {
        return responsableId;
    }

    public void setResponsableId(String responsableId) {
        this.responsableId = responsableId;
    }

    public String getResponsableNombre() {
        return responsableNombre;
    }

    public void setResponsableNombre(String responsableNombre) {
        this.responsableNombre = responsableNombre;
    }

    public String getFacturacionEstado() {
        return facturacionEstado;
    }

    public void setFacturacionEstado(String facturacionEstado) {
        this.facturacionEstado = facturacionEstado;
    }

    public Integer getFacturacionIntentos() {
        return facturacionIntentos;
    }

    public void setFacturacionIntentos(Integer facturacionIntentos) {
        this.facturacionIntentos = facturacionIntentos;
    }

    public Integer getFacturadorHttpStatus() {
        return facturadorHttpStatus;
    }

    public void setFacturadorHttpStatus(Integer facturadorHttpStatus) {
        this.facturadorHttpStatus = facturadorHttpStatus;
    }

    public String getFacturadorEndpoint() {
        return facturadorEndpoint;
    }

    public void setFacturadorEndpoint(String facturadorEndpoint) {
        this.facturadorEndpoint = facturadorEndpoint;
    }

    public String getFacturadorTipoComprobante() {
        return facturadorTipoComprobante;
    }

    public void setFacturadorTipoComprobante(String facturadorTipoComprobante) {
        this.facturadorTipoComprobante = facturadorTipoComprobante;
    }

    public String getFacturadorMensaje() {
        return facturadorMensaje;
    }

    public void setFacturadorMensaje(String facturadorMensaje) {
        this.facturadorMensaje = facturadorMensaje;
    }

    public String getFacturadorSunatEstado() {
        return facturadorSunatEstado;
    }

    public void setFacturadorSunatEstado(String facturadorSunatEstado) {
        this.facturadorSunatEstado = facturadorSunatEstado;
    }

    public String getFacturadorDocumentoId() {
        return facturadorDocumentoId;
    }

    public void setFacturadorDocumentoId(String facturadorDocumentoId) {
        this.facturadorDocumentoId = facturadorDocumentoId;
    }

    public String getFacturadorTicket() {
        return facturadorTicket;
    }

    public void setFacturadorTicket(String facturadorTicket) {
        this.facturadorTicket = facturadorTicket;
    }

    public String getFacturadorPdfUrl() {
        return facturadorPdfUrl;
    }

    public void setFacturadorPdfUrl(String facturadorPdfUrl) {
        this.facturadorPdfUrl = facturadorPdfUrl;
    }

    public String getFacturadorXmlUrl() {
        return facturadorXmlUrl;
    }

    public void setFacturadorXmlUrl(String facturadorXmlUrl) {
        this.facturadorXmlUrl = facturadorXmlUrl;
    }

    public String getFacturadorCdrUrl() {
        return facturadorCdrUrl;
    }

    public void setFacturadorCdrUrl(String facturadorCdrUrl) {
        this.facturadorCdrUrl = facturadorCdrUrl;
    }

    public String getFacturadorRespuestaJson() {
        return facturadorRespuestaJson;
    }

    public void setFacturadorRespuestaJson(String facturadorRespuestaJson) {
        this.facturadorRespuestaJson = facturadorRespuestaJson;
    }

    public OffsetDateTime getFacturacionActualizadoEn() {
        return facturacionActualizadoEn;
    }

    public void setFacturacionActualizadoEn(OffsetDateTime facturacionActualizadoEn) {
        this.facturacionActualizadoEn = facturacionActualizadoEn;
    }
}
