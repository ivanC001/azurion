package com.azurion.multitenancy;

public final class TenantContext {

    public static final String DEFAULT_TENANT = "public";
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId == null || tenantId.isBlank() ? DEFAULT_TENANT : tenantId);
    }

    public static String getTenantId() {
        String tenant = CURRENT_TENANT.get();
        return (tenant == null || tenant.isBlank()) ? DEFAULT_TENANT : tenant;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
