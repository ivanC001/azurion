package com.azurion.saascore.usuarios.domain.entities;

import com.azurion.saascore.roles.domain.entities.Rol;
import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "usuario_roles", uniqueConstraints = {
        @UniqueConstraint(name = "uq_usuario_rol", columnNames = {"usuario_id", "rol_id"})
})
public class UsuarioRol extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioTenant usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;
}
