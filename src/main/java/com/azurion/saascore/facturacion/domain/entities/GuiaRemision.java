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
}
