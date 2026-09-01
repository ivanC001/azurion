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
    private String tipoPersona = "SIN_DEFINIR";

    @Column(name = "pais_codigo", length = 2)
    private String paisCodigo;

    @Column(name = "tipo_documento", length = 30)
    private String tipoDocumento;

    @Column(name = "numero_documento", length = 30)
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

    @Column(name = "landing_key", length = 120)
    private String landingKey;

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

    @Column(name = "presupuesto_moneda", nullable = false, length = 3)
    private String presupuestoMoneda = "PEN";

    @Column(name = "fecha_interes")
    private LocalDate fechaInteres;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "catalogo_item_id")
    private Long catalogoItemId;

    @Column(name = "producto_pendiente", nullable = false)
    private boolean productoPendiente = false;

    @Column(name = "estado", nullable = false, length = 30)
    private String estado = "NUEVO";

    @Column(name = "nivel_interes", length = 20)
    private String nivelInteres = "FRIO";

    @Column(name = "necesidad_identificada", nullable = false)
    private boolean necesidadIdentificada = false;

    @Column(name = "interes_real", nullable = false, length = 20)
    private String interesReal = "BAJO";

    @Column(name = "presupuesto_definido", nullable = false, length = 20)
    private String presupuestoDefinido = "DESCONOCIDO";

    @Column(name = "tomador_decision", nullable = false, length = 20)
    private String tomadorDecision = "DESCONOCIDO";

    @Column(name = "fecha_estimada_compra", nullable = false, length = 30)
    private String fechaEstimadaCompra = "DESCONOCIDO";

    @Column(name = "score_calificacion", nullable = false)
    private Integer scoreCalificacion = 0;

    @Column(name = "temperatura", nullable = false, length = 20)
    private String temperatura = "FRIO";

    @Column(name = "motivo_espera", length = 120)
    private String motivoEspera;

    @Column(name = "fecha_proximo_contacto")
    private OffsetDateTime fechaProximoContacto;

    @Column(name = "motivo_perdida", length = 120)
    private String motivoPerdida;

    @Column(name = "observacion_perdida", length = 1000)
    private String observacionPerdida;

    @Column(name = "oportunidad_id")
    private Long oportunidadId;

    @Column(name = "responsable_id", nullable = false, length = 80)
    private String responsableId;

    @Column(name = "whatsapp_optout_en")
    private OffsetDateTime whatsappOptoutEn;

    @Column(name = "whatsapp_optout_motivo", length = 200)
    private String whatsappOptoutMotivo;

    @Column(name = "observacion", length = 1000)
    private String observacion;

    @Column(name = "cliente_id")
    private Long clienteId;

    @Column(name = "fecha_conversion")
    private OffsetDateTime fechaConversion;

    public String getTipoPersona() {
        return tipoPersona;
    }

    public void setTipoPersona(String tipoPersona) {
        this.tipoPersona = tipoPersona;
    }

    public String getPaisCodigo() {
        return paisCodigo;
    }

    public void setPaisCodigo(String paisCodigo) {
        this.paisCodigo = paisCodigo;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getNombreComercial() {
        return nombreComercial;
    }

    public void setNombreComercial(String nombreComercial) {
        this.nombreComercial = nombreComercial;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public String getCanalIngreso() {
        return canalIngreso;
    }

    public void setCanalIngreso(String canalIngreso) {
        this.canalIngreso = canalIngreso;
    }

    public String getCampania() {
        return campania;
    }

    public void setCampania(String campania) {
        this.campania = campania;
    }

    public String getLandingUrl() {
        return landingUrl;
    }

    public void setLandingUrl(String landingUrl) {
        this.landingUrl = landingUrl;
    }

    public String getLandingKey() {
        return landingKey;
    }

    public void setLandingKey(String landingKey) {
        this.landingKey = landingKey;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getTipoInteres() {
        return tipoInteres;
    }

    public void setTipoInteres(String tipoInteres) {
        this.tipoInteres = tipoInteres;
    }

    public String getInteresPrincipal() {
        return interesPrincipal;
    }

    public void setInteresPrincipal(String interesPrincipal) {
        this.interesPrincipal = interesPrincipal;
    }

    public String getInteresDetalle() {
        return interesDetalle;
    }

    public void setInteresDetalle(String interesDetalle) {
        this.interesDetalle = interesDetalle;
    }

    public BigDecimal getPresupuestoEstimado() {
        return presupuestoEstimado;
    }

    public void setPresupuestoEstimado(BigDecimal presupuestoEstimado) {
        this.presupuestoEstimado = presupuestoEstimado;
    }

    public String getPresupuestoMoneda() {
        return presupuestoMoneda;
    }

    public void setPresupuestoMoneda(String presupuestoMoneda) {
        this.presupuestoMoneda = presupuestoMoneda;
    }

    public LocalDate getFechaInteres() {
        return fechaInteres;
    }

    public void setFechaInteres(LocalDate fechaInteres) {
        this.fechaInteres = fechaInteres;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public Long getCatalogoItemId() {
        return catalogoItemId;
    }

    public void setCatalogoItemId(Long catalogoItemId) {
        this.catalogoItemId = catalogoItemId;
    }

    public boolean isProductoPendiente() {
        return productoPendiente;
    }

    public void setProductoPendiente(boolean productoPendiente) {
        this.productoPendiente = productoPendiente;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getNivelInteres() {
        return nivelInteres;
    }

    public void setNivelInteres(String nivelInteres) {
        this.nivelInteres = nivelInteres;
    }

    public boolean isNecesidadIdentificada() {
        return necesidadIdentificada;
    }

    public void setNecesidadIdentificada(boolean necesidadIdentificada) {
        this.necesidadIdentificada = handyBoolean(necesidadIdentificada);
    }

    public String getInteresReal() {
        return interesReal;
    }

    public void setInteresReal(String interesReal) {
        this.interesReal = interesReal;
    }

    public String getPresupuestoDefinido() {
        return presupuestoDefinido;
    }

    public void setPresupuestoDefinido(String presupuestoDefinido) {
        this.presupuestoDefinido = presupuestoDefinido;
    }

    public String getTomadorDecision() {
        return tomadorDecision;
    }

    public void setTomadorDecision(String tomadorDecision) {
        this.tomadorDecision = tomadorDecision;
    }

    public String getFechaEstimadaCompra() {
        return fechaEstimadaCompra;
    }

    public void setFechaEstimadaCompra(String fechaEstimadaCompra) {
        this.fechaEstimadaCompra = fechaEstimadaCompra;
    }

    public Integer getScoreCalificacion() {
        return scoreCalificacion;
    }

    public void setScoreCalificacion(Integer scoreCalificacion) {
        this.scoreCalificacion = scoreCalificacion;
    }

    public String getTemperatura() {
        return temperatura;
    }

    public void setTemperatura(String temperatura) {
        this.temperatura = temperatura;
    }

    public String getMotivoEspera() {
        return motivoEspera;
    }

    public void setMotivoEspera(String motivoEspera) {
        this.motivoEspera = motivoEspera;
    }

    public OffsetDateTime getFechaProximoContacto() {
        return fechaProximoContacto;
    }

    public void setFechaProximoContacto(OffsetDateTime fechaProximoContacto) {
        this.fechaProximoContacto = fechaProximoContacto;
    }

    public String getMotivoPerdida() {
        return motivoPerdida;
    }

    public void setMotivoPerdida(String motivoPerdida) {
        this.motivoPerdida = motivoPerdida;
    }

    public String getObservacionPerdida() {
        return observacionPerdida;
    }

    public void setObservacionPerdida(String observacionPerdida) {
        this.observacionPerdida = observacionPerdida;
    }

    public Long getOportunidadId() {
        return oportunidadId;
    }

    public void setOportunidadId(Long oportunidadId) {
        this.oportunidadId = oportunidadId;
    }

    public String getResponsableId() {
        return responsableId;
    }

    public void setResponsableId(String responsableId) {
        this.responsableId = responsableId;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public OffsetDateTime getFechaConversion() {
        return fechaConversion;
    }

    public void setFechaConversion(OffsetDateTime fechaConversion) {
        this.fechaConversion = fechaConversion;
    }

    private boolean handyBoolean(boolean b) { return b; }
}
