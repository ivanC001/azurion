package com.azurion.saascore.auth.application.services;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PermissionModulePolicy {

    private static final Set<String> TENANT_GOVERNANCE_MODULES = Set.of(
            "GENERAL", "SEGURIDAD", "CONFIGURACION", "SUCURSALES", "SAAS_CORE", "AUDITORIA"
    );
    private static final Set<String> SHARED_MODULES = Set.of("CLIENTES", "COTIZACIONES");
    private static final Set<String> ERP_MODULES = Set.of(
            "ERP", "VENTAS", "CAJA", "INVENTORY", "INVENTARIO", "PRODUCTOS", "ALMACENES",
            "COMPRAS", "REPORTES", "FACTURACION", "TRIBUTACION"
    );

    public boolean isAllowed(String permissionModule, Collection<String> activeModules) {
        String module = normalize(permissionModule);
        if (TENANT_GOVERNANCE_MODULES.contains(module)) {
            return true;
        }

        Set<String> active = activeModules == null
                ? Set.of()
                : activeModules.stream().map(this::normalize).collect(Collectors.toSet());

        if (SHARED_MODULES.contains(module)) {
            return active.contains(module);
        }
        if ("CRM".equals(module)) {
            return active.contains("CRM");
        }
        if (ERP_MODULES.contains(module)) {
            if (!active.contains("ERP")) {
                return false;
            }
            return switch (module) {
                case "ERP" -> true;
                case "INVENTORY", "INVENTARIO", "PRODUCTOS", "ALMACENES" -> active.contains("INVENTARIO");
                case "TRIBUTACION" -> active.contains("FACTURACION");
                default -> active.contains(module);
            };
        }
        return active.contains(module);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "GENERAL" : value.trim().toUpperCase(Locale.ROOT);
    }
}
