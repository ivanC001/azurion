package com.azurion.multitenancy;

import com.azurion.shared.exception.BusinessException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Keeps a tenant out of request processing when its schema could not be
 * migrated on this application instance.
 */
@Component
public class TenantMigrationReadiness {

    private final Map<String, String> failedTenants = new ConcurrentHashMap<>();

    public void markReady(String tenantId) {
        if (tenantId != null) {
            failedTenants.remove(tenantId);
        }
    }

    public void markFailed(String tenantId, Exception error) {
        if (tenantId == null || tenantId.isBlank()) {
            return;
        }
        String reason = error == null || error.getMessage() == null
                ? "Error de migracion no especificado"
                : error.getMessage();
        failedTenants.put(tenantId, reason);
    }

    public void requireReady(String tenantId) {
        if (tenantId == null || TenantContext.DEFAULT_TENANT.equalsIgnoreCase(tenantId)) {
            return;
        }
        if (failedTenants.containsKey(tenantId)) {
            throw new BusinessException(
                    "TENANT_SCHEMA_UNAVAILABLE",
                    "La base de datos de la empresa se encuentra temporalmente en mantenimiento",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        }
    }

    public Map<String, String> failures() {
        return Map.copyOf(failedTenants);
    }
}
