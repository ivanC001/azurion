package com.azurion.saascore.ubigeos.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ubigeos", schema = "public")
public class Ubigeo extends BaseEntity {

    @Column(name = "codigo", nullable = false, unique = true, length = 6)
    private String codigo;

    @Column(name = "departamento", nullable = false, length = 120)
    private String departamento;

    @Column(name = "provincia", nullable = false, length = 120)
    private String provincia;

    @Column(name = "distrito", nullable = false, length = 160)
    private String distrito;

    @Column(name = "cod_ubigeo_inei", length = 6)
    private String codUbigeoInei;

    @Column(name = "cod_ubigeo_reniec", length = 6)
    private String codUbigeoReniec;

    @Column(name = "cod_ubigeo_sunat", length = 6)
    private String codUbigeoSunat;

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

    public String getProvincia() {
        return provincia;
    }

    public void setProvincia(String provincia) {
        this.provincia = provincia;
    }

    public String getDistrito() {
        return distrito;
    }

    public void setDistrito(String distrito) {
        this.distrito = distrito;
    }

    public String getCodUbigeoInei() {
        return codUbigeoInei;
    }

    public void setCodUbigeoInei(String codUbigeoInei) {
        this.codUbigeoInei = codUbigeoInei;
    }

    public String getCodUbigeoReniec() {
        return codUbigeoReniec;
    }

    public void setCodUbigeoReniec(String codUbigeoReniec) {
        this.codUbigeoReniec = codUbigeoReniec;
    }

    public String getCodUbigeoSunat() {
        return codUbigeoSunat;
    }

    public void setCodUbigeoSunat(String codUbigeoSunat) {
        this.codUbigeoSunat = codUbigeoSunat;
    }
}
