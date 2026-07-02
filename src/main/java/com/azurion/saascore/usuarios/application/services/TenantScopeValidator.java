package com.azurion.saascore.usuarios.application.services;

import com.azurion.multitenancy.TenantContext;
import com.azurion.shared.exception.BusinessException;
import org.springframework.stereotype.Component;

@Component
public class TenantScopeValidator {

    public String requireTenantContext() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank() || TenantContext.DEFAULT_TENANT.equalsIgnoreCase(tenantId)) {
            throw new BusinessException(
                    "TENANT_REQUERIDO",
                    "Las operaciones de usuarios tenant requieren X-Tenant-Id valido y distinto de public"
            );
        }
        return tenantId;
    }

    public String requireTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank() || TenantContext.DEFAULT_TENANT.equalsIgnoreCase(tenantId)) {
            throw new BusinessException(
                    "TENANT_REQUERIDO",
                    "tenantId debe ser valido y distinto de public"
            );
        }
        return tenantId;
    }
}
