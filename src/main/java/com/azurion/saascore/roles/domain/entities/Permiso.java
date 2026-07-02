package com.azurion.saascore.roles.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "permisos")
public class Permiso extends BaseEntity {

    @Column(name = "codigo", nullable = false, length = 120, unique = true)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "descripcion", length = 400)
    private String descripcion;

    @Column(name = "modulo", nullable = false, length = 80)
    private String modulo;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "sistema", nullable = false)
    private boolean sistema;

    @OneToMany(mappedBy = "permiso")
    private Set<RolPermiso> rolPermisos = new LinkedHashSet<>();
}
