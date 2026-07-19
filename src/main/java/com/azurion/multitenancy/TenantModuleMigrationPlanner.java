package com.azurion.multitenancy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class TenantModuleMigrationPlanner {

    private static final List<String> COMMON_MIGRATIONS = List.of(
            "V4__roles_permisos_core.sql",
            "V6__tenant_admin_empresa_role.sql",
            "V7__tenant_users_and_role_seeder.sql",
            "V16__roles_permisos_business_rules.sql",
            "V38__module_contract_permissions.sql",
            "V56__separate_erp_crm_role_scopes.sql",
            "V57__retire_generic_crm_permissions.sql",
            "V60__disable_known_seeded_tenant_credentials.sql"
    );

    private static final List<String> ALL_TENANT_MIGRATIONS = List.of(
            "V1__tenant_erp_facturacion.sql",
            "V3__inventory_core.sql",
            "V4__roles_permisos_core.sql",
            "V5__caja_core.sql",
            "V6__tenant_admin_empresa_role.sql",
            "V7__tenant_users_and_role_seeder.sql",
            "V8__productos_require_almacen.sql",
            "V9__sucursales_and_cajas_by_branch.sql",
            "V10__ventas_facturacion_async_status.sql",
            "V11__guias_remision_registro_facturador_status.sql",
            "V12__notas_fiscales_credito_debito.sql",
            "V13__inventory_lotes_transferencias_ajustes.sql",
            "V14__productos_precios_stock_movimientos.sql",
            "V15__sucursales_ubigeo_igv.sql",
            "V16__roles_permisos_business_rules.sql",
            "V17__compras_lotes_origen_inventario.sql",
            "V18__clientes_datos_fiscales_credito.sql",
            "V19__cliente_abonos.sql",
            "V20__productos_fotos_texto.sql",
            "V21__effective_permissions_and_user_scopes.sql",
            "V22__warehouses_require_branch.sql",
            "V23__seed_product_categories.sql",
            "V24__cotizaciones_simples.sql",
            "V25__initialize_missing_product_stock.sql",
            "V26__arquitectura_tributaria_pos.sql",
            "V27__preserve_existing_product_tax_behavior.sql",
            "V28__crm_module.sql",
            "V29__crm_lead_capture_metadata.sql",
            "V30__crm_sales_funnel.sql",
            "V31__crm_opportunity_business_type.sql",
            "V32__crm_interest_catalog.sql",
            "V33__crm_public_catalog_security.sql",
            "V34__crm_roles_permissions.sql",
            "V35__crm_manager_catalog_permission.sql",
            "V36__crm_catalog_business_types_constraints.sql",
            "V37__crm_catalog_items_version_column.sql",
            "V38__module_contract_permissions.sql",
            "V39__crm_pipeline_governance.sql",
            "V40__crm_followup_results.sql",
            "V41__cotizaciones_crm_free_items.sql",
            "V42__cotizaciones_flujo_promociones.sql",
            "V43__crm_default_branch_support.sql",
            "V44__crm_quote_permissions.sql",
            "V45__crm_followup_qualification.sql",
            "V46__tenant_schema_drift_repair.sql",
            "V47__crm_negotiation_process.sql",
            "V48__crm_quote_product_warehouse_repair.sql",
            "V49__crm_simplified_opportunity_flow.sql",
            "V50__crm_channel_token_config.sql",
            "V51__crm_landing_config.sql",
            "V52__crm_prospect_interests.sql",
            "V53__tenant_email_config.sql",
            "V54__crm_pagination_indexes.sql",
            "V55__crm_currency_config.sql",
            "V56__separate_erp_crm_role_scopes.sql",
            "V57__retire_generic_crm_permissions.sql",
            "V58__crm_whatsapp_integration.sql",
            "V59__crm_whatsapp_conversations.sql",
            "V60__disable_known_seeded_tenant_credentials.sql",
            "V61__crm_opportunity_resources.sql",
            "V62__crm_whatsapp_connection_status.sql"
    );

    private static final Map<String, List<String>> MODULE_MIGRATIONS = buildModuleMigrations();
    private static final Map<String, Set<String>> MODULE_DEPENDENCIES = buildDependencies();

    public TenantMigrationPlan buildPlan(Collection<String> requestedModules, boolean legacyFallbackWhenEmpty) {
        Set<String> normalizedModules = normalizeModules(requestedModules);
        if (normalizedModules.isEmpty() && legacyFallbackWhenEmpty) {
            return new TenantMigrationPlan(true, List.of(), ALL_TENANT_MIGRATIONS);
        }

        if (normalizedModules.contains("ERP")) {
            return new TenantMigrationPlan(true, List.copyOf(normalizedModules), ALL_TENANT_MIGRATIONS);
        }

        LinkedHashSet<String> scripts = new LinkedHashSet<>(COMMON_MIGRATIONS);
        for (String moduleCode : expandDependencies(normalizedModules)) {
            scripts.addAll(MODULE_MIGRATIONS.getOrDefault(moduleCode, List.of()));
        }

        List<String> orderedScripts = scripts.stream()
                .sorted(Comparator.comparingInt(this::extractVersion))
                .toList();

        return new TenantMigrationPlan(false, List.copyOf(normalizedModules), orderedScripts);
    }

    public Set<String> normalizeModules(Collection<String> requestedModules) {
        if (requestedModules == null) {
            return Set.of();
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String moduleCode : requestedModules) {
            if (moduleCode == null) {
                continue;
            }
            String value = normalizeModuleCode(moduleCode);
            if (!value.isEmpty()) {
                normalized.add(value);
            }
        }
        return normalized;
    }

    private String normalizeModuleCode(String moduleCode) {
        String value = moduleCode.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "FACTURACION_CORE" -> "FACTURACION";
            case "SAAS_CORE" -> "ERP";
            default -> value;
        };
    }

    private Set<String> expandDependencies(Set<String> normalizedModules) {
        LinkedHashSet<String> expanded = new LinkedHashSet<>(normalizedModules);
        Deque<String> pending = new ArrayDeque<>(normalizedModules);

        while (!pending.isEmpty()) {
            String current = pending.removeFirst();
            for (String dependency : MODULE_DEPENDENCIES.getOrDefault(current, Set.of())) {
                if (expanded.add(dependency)) {
                    pending.addLast(dependency);
                }
            }
        }

        return expanded;
    }

    private int extractVersion(String filename) {
        int prefixStart = filename.indexOf('V');
        int prefixEnd = filename.indexOf("__");
        if (prefixStart < 0 || prefixEnd < 0 || prefixEnd <= prefixStart + 1) {
            return Integer.MAX_VALUE;
        }
        return Integer.parseInt(filename.substring(prefixStart + 1, prefixEnd));
    }

    private static Map<String, List<String>> buildModuleMigrations() {
        Map<String, List<String>> mapping = new LinkedHashMap<>();

        mapping.put("INVENTARIO", List.of(
                "V1__tenant_erp_facturacion.sql",
                "V3__inventory_core.sql",
                "V9__sucursales_and_cajas_by_branch.sql",
                "V8__productos_require_almacen.sql",
                "V13__inventory_lotes_transferencias_ajustes.sql",
                "V14__productos_precios_stock_movimientos.sql",
                "V15__sucursales_ubigeo_igv.sql",
                "V17__compras_lotes_origen_inventario.sql",
                "V20__productos_fotos_texto.sql",
                "V21__effective_permissions_and_user_scopes.sql",
                "V22__warehouses_require_branch.sql",
                "V23__seed_product_categories.sql",
                "V25__initialize_missing_product_stock.sql",
                "V48__crm_quote_product_warehouse_repair.sql"
        ));
        mapping.put("COMPRAS", List.of(
                "V17__compras_lotes_origen_inventario.sql"
        ));
        mapping.put("CLIENTES", List.of(
                "V1__tenant_erp_facturacion.sql",
                "V18__clientes_datos_fiscales_credito.sql",
                "V19__cliente_abonos.sql"
        ));
        mapping.put("VENTAS", List.of(
                "V1__tenant_erp_facturacion.sql",
                "V5__caja_core.sql",
                "V9__sucursales_and_cajas_by_branch.sql",
                "V10__ventas_facturacion_async_status.sql",
                "V15__sucursales_ubigeo_igv.sql",
                "V21__effective_permissions_and_user_scopes.sql",
                "V26__arquitectura_tributaria_pos.sql",
                "V27__preserve_existing_product_tax_behavior.sql"
        ));
        mapping.put("CAJA", List.of(
                "V5__caja_core.sql",
                "V9__sucursales_and_cajas_by_branch.sql",
                "V15__sucursales_ubigeo_igv.sql",
                "V21__effective_permissions_and_user_scopes.sql",
                "V26__arquitectura_tributaria_pos.sql"
        ));
        mapping.put("FACTURACION", List.of(
                "V1__tenant_erp_facturacion.sql",
                "V10__ventas_facturacion_async_status.sql",
                "V11__guias_remision_registro_facturador_status.sql",
                "V12__notas_fiscales_credito_debito.sql",
                "V26__arquitectura_tributaria_pos.sql",
                "V27__preserve_existing_product_tax_behavior.sql"
        ));
        mapping.put("COTIZACIONES", List.of(
                "V1__tenant_erp_facturacion.sql",
                "V9__sucursales_and_cajas_by_branch.sql",
                "V15__sucursales_ubigeo_igv.sql",
                "V21__effective_permissions_and_user_scopes.sql",
                "V24__cotizaciones_simples.sql",
                "V41__cotizaciones_crm_free_items.sql",
                "V42__cotizaciones_flujo_promociones.sql",
                "V43__crm_default_branch_support.sql",
                "V44__crm_quote_permissions.sql",
                "V45__crm_followup_qualification.sql",
                "V46__tenant_schema_drift_repair.sql",
                "V47__crm_negotiation_process.sql",
                "V48__crm_quote_product_warehouse_repair.sql"
        ));
        mapping.put("CRM", List.of(
                "V1__tenant_erp_facturacion.sql",
                "V18__clientes_datos_fiscales_credito.sql",
                "V24__cotizaciones_simples.sql",
                "V28__crm_module.sql",
                "V29__crm_lead_capture_metadata.sql",
                "V30__crm_sales_funnel.sql",
                "V31__crm_opportunity_business_type.sql",
                "V32__crm_interest_catalog.sql",
                "V33__crm_public_catalog_security.sql",
                "V34__crm_roles_permissions.sql",
                "V35__crm_manager_catalog_permission.sql",
                "V36__crm_catalog_business_types_constraints.sql",
                "V37__crm_catalog_items_version_column.sql",
                "V39__crm_pipeline_governance.sql",
                "V40__crm_followup_results.sql",
                "V41__cotizaciones_crm_free_items.sql",
                "V42__cotizaciones_flujo_promociones.sql",
                "V43__crm_default_branch_support.sql",
                "V44__crm_quote_permissions.sql",
                "V45__crm_followup_qualification.sql",
                "V46__tenant_schema_drift_repair.sql",
                "V47__crm_negotiation_process.sql",
                "V48__crm_quote_product_warehouse_repair.sql",
                "V49__crm_simplified_opportunity_flow.sql",
                "V50__crm_channel_token_config.sql",
                "V51__crm_landing_config.sql",
                "V52__crm_prospect_interests.sql",
                "V53__tenant_email_config.sql",
                "V54__crm_pagination_indexes.sql",
                "V55__crm_currency_config.sql",
                "V58__crm_whatsapp_integration.sql",
                "V59__crm_whatsapp_conversations.sql",
                "V60__disable_known_seeded_tenant_credentials.sql",
                "V61__crm_opportunity_resources.sql",
                "V62__crm_whatsapp_connection_status.sql"
        ));
        mapping.put("REPORTES", List.of());
        return mapping;
    }

    private static Map<String, Set<String>> buildDependencies() {
        Map<String, Set<String>> dependencies = new LinkedHashMap<>();
        dependencies.put("CRM", Set.of("CLIENTES", "COTIZACIONES"));
        dependencies.put("FACTURACION", Set.of("CLIENTES", "VENTAS"));
        dependencies.put("VENTAS", Set.of("CLIENTES"));
        dependencies.put("CAJA", Set.of("VENTAS"));
        dependencies.put("COMPRAS", Set.of("INVENTARIO"));
        dependencies.put("COTIZACIONES", Set.of("CLIENTES"));
        return dependencies;
    }
}
