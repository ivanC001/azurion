package com.azurion.saascore.auth.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "usuarios_globales", schema = "public")
public class UsuarioGlobal extends BaseEntity {

    @Column(name = "username", nullable = false, unique = true, length = 120)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "roles", nullable = false, length = 255)
    private String roles;

    @Column(name = "empresa_default", length = 80)
    private String empresaDefault;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public String getEmpresaDefault() {
        return empresaDefault;
    }

    public void setEmpresaDefault(String empresaDefault) {
        this.empresaDefault = empresaDefault;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
