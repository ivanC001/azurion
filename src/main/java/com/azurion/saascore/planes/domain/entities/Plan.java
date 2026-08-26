package com.azurion.saascore.planes.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "planes", schema = "public")
public class Plan extends BaseEntity {

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "codigo", nullable = false, unique = true, length = 40)
    private String codigo;

    @Column(name = "descripcion", length = 400)
    private String descripcion;

    @Column(name = "limite_mensual_bolsa", nullable = false)
    private Long limiteMensualBolsa;

    @Column(name = "limite_usuarios", nullable = false)
    private Integer limiteUsuarios;

    @Column(name = "precio_mensual", nullable = false, precision = 18, scale = 2)
    private BigDecimal precioMensual;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Long getLimiteMensualBolsa() {
        return limiteMensualBolsa;
    }

    public void setLimiteMensualBolsa(Long limiteMensualBolsa) {
        this.limiteMensualBolsa = limiteMensualBolsa;
    }

    public Integer getLimiteUsuarios() {
        return limiteUsuarios;
    }

    public void setLimiteUsuarios(Integer limiteUsuarios) {
        this.limiteUsuarios = limiteUsuarios;
    }

    public BigDecimal getPrecioMensual() {
        return precioMensual;
    }

    public void setPrecioMensual(BigDecimal precioMensual) {
        this.precioMensual = precioMensual;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
