package com.azurion.multitenancy;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "schemas_empresas", schema = "public")
public class TenantSchemaRegistry extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, unique = true, length = 80)
    private String tenantId;

    @Column(name = "schema_name", nullable = false, unique = true, length = 80)
    private String schemaName;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
