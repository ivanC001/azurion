package com.azurion.saascore.inventory.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "unidades_medida")
public class UnidadMedida extends BaseEntity {

    @Column(name = "codigo_sunat", nullable = false, unique = true, length = 12)
    private String codigoSunat;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "abreviatura", nullable = false, length = 20)
    private String abreviatura;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "ACTIVO";

    public String getCodigoSunat() {
        return codigoSunat;
    }

    public void setCodigoSunat(String codigoSunat) {
        this.codigoSunat = codigoSunat;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getAbreviatura() {
        return abreviatura;
    }

    public void setAbreviatura(String abreviatura) {
        this.abreviatura = abreviatura;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
