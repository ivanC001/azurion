package com.azurion.saascore.ventas.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ventas")
public class Venta extends BaseEntity {

    public static final String FACTURACION_ESTADO_PENDIENTE = "PENDIENTE";
    public static final String FACTURACION_ESTADO_NO_REQUIERE = "NO_REQUIERE";
    public static final String FACTURACION_ESTADO_PROCESANDO = "PROCESANDO";
    public static final String FACTURACION_ESTADO_ACEPTADO = "ACEPTADO";
    public static final String FACTURACION_ESTADO_RECHAZADO = "RECHAZADO";
    public static final String FACTURACION_ESTADO_ERROR = "ERROR";

    @Column(name = "external_id", nullable = false, unique = true, length = 80)
    private String externalId;

    @Column(name = "cliente_documento", nullable = false, length = 20)
    private String clienteDocumento;

    @Column(name = "cliente_nombre", nullable = false, length = 255)
    private String clienteNombre;

    @Column(name = "moneda", nullable = false, length = 3)
    private String moneda;

    @Column(name = "total", nullable = false, precision = 18, scale = 2)
    private BigDecimal total;

    @Column(name = "fecha_venta", nullable = false)
    private OffsetDateTime fechaVenta;

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
