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
}
