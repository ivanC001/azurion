package com.azurion.saascore.crm.domain.entities;

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
@Table(name = "crm_prospectos")
public class CrmProspecto extends BaseEntity {

    @Column(name = "tipo_persona", nullable = false, length = 20)
    private String tipoPersona;

    @Column(name = "tipo_documento", length = 5)
    private String tipoDocumento;

    @Column(name = "numero_documento", length = 20)
    private String numeroDocumento;

    @Column(name = "nombre", nullable = false, length = 180)
    private String nombre;

    @Column(name = "razon_social", length = 220)
    private String razonSocial;

    @Column(name = "nombre_comercial", length = 180)
    private String nombreComercial;

    @Column(name = "telefono", length = 40)
    private String telefono;

    @Column(name = "correo", length = 180)
    private String correo;

    @Column(name = "direccion", length = 500)
    private String direccion;

    @Column(name = "origen", nullable = false, length = 30)
    private String origen;

    @Column(name = "canal_ingreso", nullable = false, length = 30)
    private String canalIngreso = "MANUAL";

    @Column(name = "campania", length = 120)
    private String campania;

    @Column(name = "landing_url", length = 500)
    private String landingUrl;

    @Column(name = "mensaje", length = 1500)
    private String mensaje;

    @Column(name = "tipo_interes", nullable = false, length = 30)
    private String tipoInteres = "PRODUCTO";

    @Column(name = "interes_principal", length = 220)
    private String interesPrincipal;

    @Column(name = "interes_detalle", length = 1500)
    private String interesDetalle;

    @Column(name = "presupuesto_estimado", precision = 18, scale = 2)
    private BigDecimal presupuestoEstimado;

    @Column(name = "fecha_interes")
    private LocalDate fechaInteres;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "catalogo_item_id")
    private Long catalogoItemId;

    @Column(name = "estado", nullable = false, length = 30)
    private String estado = "NUEVO";

    @Column(name = "nivel_interes", length = 20)
    private String nivelInteres = "FRIO";

    @Column(name = "responsable_id", nullable = false, length = 80)
    private String responsableId;

    @Column(name = "observacion", length = 1000)
    private String observacion;

    @Column(name = "cliente_id")
    private Long clienteId;

    @Column(name = "fecha_conversion")
    private OffsetDateTime fechaConversion;
}
