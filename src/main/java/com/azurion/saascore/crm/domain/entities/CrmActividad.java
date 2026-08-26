package com.azurion.saascore.crm.domain.entities;

import com.azurion.saascore.clientes.domain.entities.Cliente;
import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "crm_actividades")
public class CrmActividad extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prospecto_id")
    private CrmProspecto prospecto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oportunidad_id")
    private CrmOportunidad oportunidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(name = "tipo_actividad", nullable = false, length = 30)
    private String tipoActividad;

    @Column(name = "asunto", nullable = false, length = 220)
    private String asunto;

    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    @Column(name = "fecha_programada", nullable = false)
    private OffsetDateTime fechaProgramada;

    @Column(name = "fecha_realizada")
    private OffsetDateTime fechaRealizada;

    @Column(name = "estado", nullable = false, length = 30)
    private String estado = "PENDIENTE";

    @Column(name = "usuario_id", nullable = false, length = 80)
    private String usuarioId;

    @Column(name = "resultado", length = 1000)
    private String resultado;

    @Column(name = "resultado_contacto", length = 40)
    private String resultadoContacto;

    @Column(name = "nivel_interes", length = 20)
    private String nivelInteres;

    @Column(name = "estado_prospecto_resultado", length = 30)
    private String estadoProspectoResultado;

    public CrmProspecto getProspecto() {
        return prospecto;
    }

    public void setProspecto(CrmProspecto prospecto) {
        this.prospecto = prospecto;
    }

    public CrmOportunidad getOportunidad() {
        return oportunidad;
    }

    public void setOportunidad(CrmOportunidad oportunidad) {
        this.oportunidad = oportunidad;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public String getTipoActividad() {
        return tipoActividad;
    }

    public void setTipoActividad(String tipoActividad) {
        this.tipoActividad = tipoActividad;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public OffsetDateTime getFechaProgramada() {
        return fechaProgramada;
    }

    public void setFechaProgramada(OffsetDateTime fechaProgramada) {
        this.fechaProgramada = fechaProgramada;
    }

    public OffsetDateTime getFechaRealizada() {
        return fechaRealizada;
    }

    public void setFechaRealizada(OffsetDateTime fechaRealizada) {
        this.fechaRealizada = fechaRealizada;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    public String getResultadoContacto() {
        return resultadoContacto;
    }

    public void setResultadoContacto(String resultadoContacto) {
        this.resultadoContacto = resultadoContacto;
    }

    public String getNivelInteres() {
        return nivelInteres;
    }

    public void setNivelInteres(String nivelInteres) {
        this.nivelInteres = nivelInteres;
    }

    public String getEstadoProspectoResultado() {
        return estadoProspectoResultado;
    }

    public void setEstadoProspectoResultado(String estadoProspectoResultado) {
        this.estadoProspectoResultado = estadoProspectoResultado;
    }
}
