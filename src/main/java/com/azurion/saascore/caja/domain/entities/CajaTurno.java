package com.azurion.saascore.caja.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "caja_turnos")
public class CajaTurno extends BaseEntity {

    @Column(name = "numero", nullable = false, unique = true, length = 30)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caja_id", nullable = false)
    private CajaFisica caja;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "moneda", nullable = false, length = 3)
    private String moneda;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "fecha_apertura", nullable = false)
    private OffsetDateTime fechaApertura;

    @Column(name = "fecha_cierre")
    private OffsetDateTime fechaCierre;

    @Column(name = "saldo_apertura", nullable = false, precision = 18, scale = 2)
    private BigDecimal saldoApertura;

    @Column(name = "saldo_esperado", nullable = false, precision = 18, scale = 2)
    private BigDecimal saldoEsperado;

    @Column(name = "conteo_fisico", precision = 18, scale = 2)
    private BigDecimal conteoFisico;

    @Column(name = "diferencia_cierre", precision = 18, scale = 2)
    private BigDecimal diferenciaCierre;

    @Column(name = "numero_ventas", nullable = false)
    private Integer numeroVentas = 0;

    @Column(name = "total_ventas", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalVentas = BigDecimal.ZERO;

    @Column(name = "total_efectivo", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalEfectivo = BigDecimal.ZERO;

    @Column(name = "total_tarjeta", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalTarjeta = BigDecimal.ZERO;

    @Column(name = "total_billetera_digital", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalBilleteraDigital = BigDecimal.ZERO;

    @Column(name = "total_transferencia", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalTransferencia = BigDecimal.ZERO;

    @Column(name = "total_credito", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalCredito = BigDecimal.ZERO;

    @Column(name = "total_ingresos_manuales", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalIngresosManuales = BigDecimal.ZERO;

    @Column(name = "total_retiros", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalRetiros = BigDecimal.ZERO;

    @Column(name = "total_depositos", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalDepositos = BigDecimal.ZERO;

    @Column(name = "total_reembolsos", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalReembolsos = BigDecimal.ZERO;

    @Column(name = "responsable_apertura_id", nullable = false, length = 80)
    private String responsableAperturaId;

    @Column(name = "responsable_apertura_nombre", nullable = false, length = 150)
    private String responsableAperturaNombre;

    @Column(name = "responsable_cierre_id", length = 80)
    private String responsableCierreId;

    @Column(name = "responsable_cierre_nombre", length = 150)
    private String responsableCierreNombre;

    @Column(name = "observacion_apertura", length = 500)
    private String observacionApertura;

    @Column(name = "observacion_cierre", length = 500)
    private String observacionCierre;

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public CajaFisica getCaja() {
        return caja;
    }

    public void setCaja(CajaFisica caja) {
        this.caja = caja;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public OffsetDateTime getFechaApertura() {
        return fechaApertura;
    }

    public void setFechaApertura(OffsetDateTime fechaApertura) {
        this.fechaApertura = fechaApertura;
    }

    public OffsetDateTime getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(OffsetDateTime fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public BigDecimal getSaldoApertura() {
        return saldoApertura;
    }

    public void setSaldoApertura(BigDecimal saldoApertura) {
        this.saldoApertura = saldoApertura;
    }

    public BigDecimal getSaldoEsperado() {
        return saldoEsperado;
    }

    public void setSaldoEsperado(BigDecimal saldoEsperado) {
        this.saldoEsperado = saldoEsperado;
    }

    public BigDecimal getConteoFisico() {
        return conteoFisico;
    }

    public void setConteoFisico(BigDecimal conteoFisico) {
        this.conteoFisico = conteoFisico;
    }

    public BigDecimal getDiferenciaCierre() {
        return diferenciaCierre;
    }

    public void setDiferenciaCierre(BigDecimal diferenciaCierre) {
        this.diferenciaCierre = diferenciaCierre;
    }

    public Integer getNumeroVentas() {
        return numeroVentas;
    }

    public void setNumeroVentas(Integer numeroVentas) {
        this.numeroVentas = numeroVentas;
    }

    public BigDecimal getTotalVentas() {
        return totalVentas;
    }

    public void setTotalVentas(BigDecimal totalVentas) {
        this.totalVentas = totalVentas;
    }

    public BigDecimal getTotalEfectivo() {
        return totalEfectivo;
    }

    public void setTotalEfectivo(BigDecimal totalEfectivo) {
        this.totalEfectivo = totalEfectivo;
    }

    public BigDecimal getTotalTarjeta() {
        return totalTarjeta;
    }

    public void setTotalTarjeta(BigDecimal totalTarjeta) {
        this.totalTarjeta = totalTarjeta;
    }

    public BigDecimal getTotalBilleteraDigital() {
        return totalBilleteraDigital;
    }

    public void setTotalBilleteraDigital(BigDecimal totalBilleteraDigital) {
        this.totalBilleteraDigital = totalBilleteraDigital;
    }

    public BigDecimal getTotalTransferencia() {
        return totalTransferencia;
    }

    public void setTotalTransferencia(BigDecimal totalTransferencia) {
        this.totalTransferencia = totalTransferencia;
    }

    public BigDecimal getTotalCredito() {
        return totalCredito;
    }

    public void setTotalCredito(BigDecimal totalCredito) {
        this.totalCredito = totalCredito;
    }

    public BigDecimal getTotalIngresosManuales() {
        return totalIngresosManuales;
    }

    public void setTotalIngresosManuales(BigDecimal totalIngresosManuales) {
        this.totalIngresosManuales = totalIngresosManuales;
    }

    public BigDecimal getTotalRetiros() {
        return totalRetiros;
    }

    public void setTotalRetiros(BigDecimal totalRetiros) {
        this.totalRetiros = totalRetiros;
    }

    public BigDecimal getTotalDepositos() {
        return totalDepositos;
    }

    public void setTotalDepositos(BigDecimal totalDepositos) {
        this.totalDepositos = totalDepositos;
    }

    public BigDecimal getTotalReembolsos() {
        return totalReembolsos;
    }

    public void setTotalReembolsos(BigDecimal totalReembolsos) {
        this.totalReembolsos = totalReembolsos;
    }

    public String getResponsableAperturaId() {
        return responsableAperturaId;
    }

    public void setResponsableAperturaId(String responsableAperturaId) {
        this.responsableAperturaId = responsableAperturaId;
    }

    public String getResponsableAperturaNombre() {
        return responsableAperturaNombre;
    }

    public void setResponsableAperturaNombre(String responsableAperturaNombre) {
        this.responsableAperturaNombre = responsableAperturaNombre;
    }

    public String getResponsableCierreId() {
        return responsableCierreId;
    }

    public void setResponsableCierreId(String responsableCierreId) {
        this.responsableCierreId = responsableCierreId;
    }

    public String getResponsableCierreNombre() {
        return responsableCierreNombre;
    }

    public void setResponsableCierreNombre(String responsableCierreNombre) {
        this.responsableCierreNombre = responsableCierreNombre;
    }

    public String getObservacionApertura() {
        return observacionApertura;
    }

    public void setObservacionApertura(String observacionApertura) {
        this.observacionApertura = observacionApertura;
    }

    public String getObservacionCierre() {
        return observacionCierre;
    }

    public void setObservacionCierre(String observacionCierre) {
        this.observacionCierre = observacionCierre;
    }
}
