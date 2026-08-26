package com.azurion.saascore.usuarios.domain.entities;

import com.azurion.saascore.auth.domain.entities.UsuarioGlobal;
import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "usuario_global_roles", schema = "public")
public class UsuarioGlobalRol extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_global_id", nullable = false)
    private UsuarioGlobal usuarioGlobal;

    @Column(name = "rol_codigo", nullable = false, length = 80)
    private String rolCodigo;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    public UsuarioGlobal getUsuarioGlobal() {
        return usuarioGlobal;
    }

    public void setUsuarioGlobal(UsuarioGlobal usuarioGlobal) {
        this.usuarioGlobal = usuarioGlobal;
    }

    public String getRolCodigo() {
        return rolCodigo;
    }

    public void setRolCodigo(String rolCodigo) {
        this.rolCodigo = rolCodigo;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
