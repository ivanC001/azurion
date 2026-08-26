package com.azurion.saascore.facturacion.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "guias_remision")
public class GuiaRemision extends BaseEntity {

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

    @Column(name = "sucursal_origen_id", nullable = false)
    private Long sucursalOrigenId;

    @Column(name = "sucursal_origen_nombre", nullable = false, length = 255)
    private String sucursalOrigenNombre;

    @Column(name = "sucursal_destino_id", nullable = false)
    private Long sucursalDestinoId;

    @Column(name = "sucursal_destino_nombre", nullable = false, length = 255)
    private String sucursalDestinoNombre;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "fecha_traslado", nullable = false)
    private LocalDate fechaTraslado;

    @Column(name = "motivo_traslado", length = 120)
    private String motivoTraslado;

    @Column(name = "transportista", length = 255)
    private String transportista;

    @Column(name = "observacion", length = 500)
    private String observacion;

    @Column(name = "responsable_id", nullable = false, length = 120)
    private String responsableId;

    @Column(name = "responsable_nombre", nullable = false, length = 255)
    private String responsableNombre;

    @Column(name = "items_resumen", columnDefinition = "TEXT")
    private String itemsResumen;

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

    public Long getSucursalOrigenId() {
        return sucursalOrigenId;
    }

    public void setSucursalOrigenId(Long sucursalOrigenId) {
        this.sucursalOrigenId = sucursalOrigenId;
    }

    public String getSucursalOrigenNombre() {
        return sucursalOrigenNombre;
    }

    public void setSucursalOrigenNombre(String sucursalOrigenNombre) {
        this.sucursalOrigenNombre = sucursalOrigenNombre;
    }

    public Long getSucursalDestinoId() {
        return sucursalDestinoId;
    }

    public void setSucursalDestinoId(Long sucursalDestinoId) {
        this.sucursalDestinoId = sucursalDestinoId;
    }

    public String getSucursalDestinoNombre() {
        return sucursalDestinoNombre;
    }

    public void setSucursalDestinoNombre(String sucursalDestinoNombre) {
        this.sucursalDestinoNombre = sucursalDestinoNombre;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDate fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public LocalDate getFechaTraslado() {
        return fechaTraslado;
    }

    public void setFechaTraslado(LocalDate fechaTraslado) {
        this.fechaTraslado = fechaTraslado;
    }

    public String getMotivoTraslado() {
        return motivoTraslado;
    }

    public void setMotivoTraslado(String motivoTraslado) {
        this.motivoTraslado = motivoTraslado;
    }

    public String getTransportista() {
        return transportista;
    }

    public void setTransportista(String transportista) {
        this.transportista = transportista;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
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

    public String getItemsResumen() {
        return itemsResumen;
    }

    public void setItemsResumen(String itemsResumen) {
        this.itemsResumen = itemsResumen;
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
