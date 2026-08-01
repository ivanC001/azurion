package com.azurion.saascore.empresas.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "empresas", schema = "public")
public class Empresa extends BaseEntity {
    public static final String FACTURADOR_STATUS_NO_REQUERIDO = "NO_REQUERIDO";
    public static final String FACTURADOR_STATUS_PENDIENTE = "PENDIENTE";
    public static final String FACTURADOR_STATUS_PROVISIONANDO = "PROVISIONANDO";
    public static final String FACTURADOR_STATUS_PROVISIONADO = "PROVISIONADO";
    public static final String FACTURADOR_STATUS_REINTENTO = "REINTENTO";
    public static final String FACTURADOR_STATUS_ERROR = "ERROR";
    public static final String FACTURADOR_STATUS_SUSPENDIDO = "SUSPENDIDO";

    public static final String FACTURADOR_DOCUMENT_MODE_TICKET_ONLY = "TICKET_ONLY";
    public static final String FACTURADOR_DOCUMENT_MODE_ELECTRONIC = "ELECTRONIC";
    public static final String FACTURADOR_FISCAL_STATUS_NOT_CONFIGURED = "NOT_CONFIGURED";
    public static final String FACTURADOR_FISCAL_STATUS_ACTIVE = "ACTIVE";
    public static final String FACTURADOR_FISCAL_STATUS_SUSPENDED = "SUSPENDED";
    public static final String FACTURADOR_SUNAT_MODE_DISABLED = "DISABLED";
    public static final String FACTURADOR_SUNAT_MODE_BETA = "BETA";
    public static final String FACTURADOR_SUNAT_MODE_PRODUCTION = "PRODUCTION";

    @Column(name = "ruc", nullable = false, unique = true, length = 40)
    private String ruc;

    @Column(name = "razon_social", nullable = false, length = 255)
    private String razonSocial;

    @Column(name = "tipo_documento_fiscal", nullable = false, length = 30)
    private String tipoDocumentoFiscal = "RUC";

    @Column(name = "nombre_comercial", length = 180)
    private String nombreComercial;

    @Column(name = "direccion_fiscal", length = 500)
    private String direccionFiscal;

    @Column(name = "distrito", length = 120)
    private String distrito;

    @Column(name = "provincia", length = 120)
    private String provincia;

    @Column(name = "departamento", length = 120)
    private String departamento;

    @Column(name = "pais_codigo", nullable = false, length = 2)
    private String paisCodigo = "PE";

    @Column(name = "pais_nombre", nullable = false, length = 100)
    private String paisNombre = "Peru";

    @Column(name = "correo_principal", length = 180)
    private String correoPrincipal;

    @Column(name = "telefono", length = 40)
    private String telefono;

    @Column(name = "celular", length = 40)
    private String celular;

    @Column(name = "sitio_web", length = 300)
    private String sitioWeb;

    @Column(name = "facebook", length = 300)
    private String facebook;

    @Column(name = "instagram", length = 300)
    private String instagram;

    @Column(name = "representante_nombre", length = 180)
    private String representanteNombre;

    @Column(name = "representante_tipo_documento", length = 30)
    private String representanteTipoDocumento;

    @Column(name = "representante_numero_documento", length = 40)
    private String representanteNumeroDocumento;

    @Column(name = "representante_cargo", length = 120)
    private String representanteCargo;

    @Column(name = "representante_correo", length = 180)
    private String representanteCorreo;

    @Column(name = "representante_telefono", length = 40)
    private String representanteTelefono;

    @Column(name = "zona_horaria", nullable = false, length = 80)
    private String zonaHoraria = "America/Lima";

    @Column(name = "idioma", nullable = false, length = 20)
    private String idioma = "es-PE";

    @Column(name = "formato_fecha", nullable = false, length = 20)
    private String formatoFecha = "DD/MM/YYYY";

    @Column(name = "formato_hora", nullable = false, length = 10)
    private String formatoHora = "24H";

    @Column(name = "moneda_codigo", nullable = false, length = 3)
    private String monedaCodigo = "PEN";

    @Column(name = "moneda_simbolo", nullable = false, length = 10)
    private String monedaSimbolo = "S/";

    @Column(name = "tenant_id", nullable = false, unique = true, length = 80)
    private String tenantId;

    @Column(name = "schema_name", nullable = false, unique = true, length = 80)
    private String schemaName;

    @Column(name = "logo_panel_url", length = 500)
    private String logoPanelUrl;

    @Column(name = "facturador_status", nullable = false, length = 30)
    private String facturadorStatus = FACTURADOR_STATUS_NO_REQUERIDO;

    @Column(name = "facturador_document_mode", nullable = false, length = 30)
    private String facturadorDocumentMode = FACTURADOR_DOCUMENT_MODE_TICKET_ONLY;

    @Column(name = "facturador_fiscal_status", nullable = false, length = 30)
    private String facturadorFiscalStatus = FACTURADOR_FISCAL_STATUS_NOT_CONFIGURED;

    @Column(name = "facturador_sunat_mode", nullable = false, length = 20)
    private String facturadorSunatMode = FACTURADOR_SUNAT_MODE_DISABLED;

    @Column(name = "facturador_last_error", length = 1000)
    private String facturadorLastError;

    @Column(name = "facturador_provisioned_at")
    private OffsetDateTime facturadorProvisionedAt;

    @Column(name = "facturador_next_attempt_at")
    private OffsetDateTime facturadorNextAttemptAt;

    @Column(name = "facturador_attempts", nullable = false)
    private Integer facturadorAttempts = 0;

    @Column(name = "facturador_lease_owner", length = 120)
    private String facturadorLeaseOwner;

    @Column(name = "facturador_lease_until")
    private OffsetDateTime facturadorLeaseUntil;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;
}
