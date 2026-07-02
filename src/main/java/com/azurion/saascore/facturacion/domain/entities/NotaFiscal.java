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
}
