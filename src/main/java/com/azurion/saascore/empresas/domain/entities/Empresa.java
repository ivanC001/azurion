package com.azurion.saascore.empresas.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "empresas", schema = "public")
public class Empresa extends BaseEntity {

    @Column(name = "ruc", nullable = false, unique = true, length = 11)
    private String ruc;

    @Column(name = "razon_social", nullable = false, length = 255)
    private String razonSocial;

    @Column(name = "tenant_id", nullable = false, unique = true, length = 80)
    private String tenantId;

    @Column(name = "schema_name", nullable = false, unique = true, length = 80)
    private String schemaName;

    @Column(name = "logo_panel_url", length = 500)
    private String logoPanelUrl;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;
}
