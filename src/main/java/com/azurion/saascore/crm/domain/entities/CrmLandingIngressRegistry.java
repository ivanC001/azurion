package com.azurion.saascore.crm.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "crm_landing_ingress_registry",
        schema = "public",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_crm_landing_ingress_tenant_config",
                columnNames = {"tenant_id", "landing_config_id"}
        )
)
public class CrmLandingIngressRegistry extends BaseEntity {

    @Column(name = "source_key", nullable = false, unique = true, length = 120)
    private String sourceKey;

    @Column(name = "tenant_id", nullable = false, length = 80)
    private String tenantId;

    @Column(name = "landing_config_id", nullable = false)
    private Long landingConfigId;

    @Column(name = "relay_secret_encrypted", nullable = false, columnDefinition = "TEXT")
    private String relaySecretEncrypted;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    public String getSourceKey() {
        return sourceKey;
    }

    public void setSourceKey(String sourceKey) {
        this.sourceKey = sourceKey;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Long getLandingConfigId() {
        return landingConfigId;
    }

    public void setLandingConfigId(Long landingConfigId) {
        this.landingConfigId = landingConfigId;
    }

    public String getRelaySecretEncrypted() {
        return relaySecretEncrypted;
    }

    public void setRelaySecretEncrypted(String relaySecretEncrypted) {
        this.relaySecretEncrypted = relaySecretEncrypted;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
