package com.azurion.saascore.crm.domain.entities;

import com.azurion.saascore.clientes.domain.entities.Cliente;
import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "crm_oportunidades")
public class CrmOportunidad extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prospecto_id")
    private CrmProspecto prospecto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(name = "titulo", nullable = false, length = 220)
    private String titulo;

    @Column(name = "tipo_oportunidad", nullable = false, length = 30)
    private String tipoOportunidad = "PRODUCTO";

    @Column(name = "catalogo_item_id")
    private Long catalogoItemId;

    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    @Column(name = "monto_estimado", nullable = false, precision = 18, scale = 2)
    private BigDecimal montoEstimado = BigDecimal.ZERO;

    @Column(name = "moneda", nullable = false, length = 3)
    private String moneda = "PEN";

    @Column(name = "probabilidad", nullable = false)
    private Integer probabilidad = 0;

    @Column(name = "etapa", nullable = false, length = 30)
    private String etapa = "NUEVO";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etapa_id", nullable = false)
    private CrmEtapaPipeline etapaPipeline;

    @Column(name = "fecha_cierre_estimada")
    private LocalDate fechaCierreEstimada;

    @Column(name = "responsable_id", nullable = false, length = 80)
    private String responsableId;

    @Column(name = "estado", nullable = false, length = 30)
    private String estado = "ABIERTA";

    @Column(name = "motivo_perdida", length = 500)
    private String motivoPerdida;

    @Column(name = "fecha_cierre_real")
    private OffsetDateTime fechaCierreReal;

    @Column(name = "fecha_ultima_actualizacion")
    private OffsetDateTime fechaUltimaActualizacion;

    @Column(name = "fecha_ganada")
    private OffsetDateTime fechaGanada;

    @Column(name = "fecha_perdida")
    private OffsetDateTime fechaPerdida;

    @Column(name = "monto_real", precision = 18, scale = 2)
    private BigDecimal montoReal;

    public CrmProspecto getProspecto() {
        return prospecto;
    }

    public void setProspecto(CrmProspecto prospecto) {
        this.prospecto = prospecto;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getTipoOportunidad() {
        return tipoOportunidad;
    }

    public void setTipoOportunidad(String tipoOportunidad) {
        this.tipoOportunidad = tipoOportunidad;
    }

    public Long getCatalogoItemId() {
        return catalogoItemId;
    }

    public void setCatalogoItemId(Long catalogoItemId) {
        this.catalogoItemId = catalogoItemId;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getMontoEstimado() {
        return montoEstimado;
    }

    public void setMontoEstimado(BigDecimal montoEstimado) {
        this.montoEstimado = montoEstimado;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public Integer getProbabilidad() {
        return probabilidad;
    }

    public void setProbabilidad(Integer probabilidad) {
        this.probabilidad = probabilidad;
    }

    public String getEtapa() {
        return etapa;
    }

    public void setEtapa(String etapa) {
        this.etapa = etapa;
    }

    public CrmEtapaPipeline getEtapaPipeline() {
        return etapaPipeline;
    }

    public void setEtapaPipeline(CrmEtapaPipeline etapaPipeline) {
        this.etapaPipeline = etapaPipeline;
    }

    public LocalDate getFechaCierreEstimada() {
        return fechaCierreEstimada;
    }

    public void setFechaCierreEstimada(LocalDate fechaCierreEstimada) {
        this.fechaCierreEstimada = fechaCierreEstimada;
    }

    public String getResponsableId() {
        return responsableId;
    }

    public void setResponsableId(String responsableId) {
        this.responsableId = responsableId;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getMotivoPerdida() {
        return motivoPerdida;
    }

    public void setMotivoPerdida(String motivoPerdida) {
        this.motivoPerdida = motivoPerdida;
    }

    public OffsetDateTime getFechaCierreReal() {
        return fechaCierreReal;
    }

    public void setFechaCierreReal(OffsetDateTime fechaCierreReal) {
        this.fechaCierreReal = fechaCierreReal;
    }

    public OffsetDateTime getFechaUltimaActualizacion() {
        return fechaUltimaActualizacion;
    }

    public void setFechaUltimaActualizacion(OffsetDateTime fechaUltimaActualizacion) {
        this.fechaUltimaActualizacion = fechaUltimaActualizacion;
    }

    public OffsetDateTime getFechaGanada() {
        return fechaGanada;
    }

    public void setFechaGanada(OffsetDateTime fechaGanada) {
        this.fechaGanada = fechaGanada;
    }

    public OffsetDateTime getFechaPerdida() {
        return fechaPerdida;
    }

    public void setFechaPerdida(OffsetDateTime fechaPerdida) {
        this.fechaPerdida = fechaPerdida;
    }

    public BigDecimal getMontoReal() {
        return montoReal;
    }

    public void setMontoReal(BigDecimal montoReal) {
        this.montoReal = montoReal;
    }
}
