package com.azurion.saascore.roles.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "roles")
public class Rol extends BaseEntity {

    @Column(name = "codigo", nullable = false, length = 80, unique = true)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "descripcion", length = 400)
    private String descripcion;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "sistema", nullable = false)
    private boolean sistema;

    @Column(name = "deprecated", nullable = false)
    private boolean deprecated;

    @Enumerated(EnumType.STRING)
    @Column(name = "ambito", nullable = false, length = 20)
    private RoleScope ambito = RoleScope.ERP;

    @OneToMany(mappedBy = "rol", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RolPermiso> rolPermisos = new LinkedHashSet<>();
}
