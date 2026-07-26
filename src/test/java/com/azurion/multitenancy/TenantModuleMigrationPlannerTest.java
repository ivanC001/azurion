package com.azurion.multitenancy;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class TenantModuleMigrationPlannerTest {

    private final TenantModuleMigrationPlanner planner = new TenantModuleMigrationPlanner();

    @Test
    void includesWhatsappConnectionStatusForCrmTenants() {
        TenantMigrationPlan plan = planner.buildPlan(List.of("CRM"), false);

        assertTrue(plan.scriptNames().contains("V62__crm_whatsapp_connection_status.sql"));
        assertTrue(plan.scriptNames().contains("V63__cotizacion_email_send_guard.sql"));
        assertTrue(plan.scriptNames().contains("V65__crm_whatsapp_delivery_status.sql"));
        assertTrue(plan.scriptNames().contains("V67__activate_default_tenant_admin.sql"));
        assertTrue(plan.scriptNames().contains("V68__crm_landing_optional_product_default.sql"));
        assertTrue(plan.scriptNames().contains("V69__crm_sent_email_inbox_index.sql"));
        assertTrue(plan.scriptNames().contains("V70__reconcile_default_tenant_admin.sql"));
        assertTrue(plan.scriptNames().contains("V71__crm_whatsapp_internal_notes.sql"));
        assertTrue(plan.scriptNames().contains("V73__harden_public_lead_ingress.sql"));
        assertTrue(plan.scriptNames().contains("V74__audit_public_lead_rejections.sql"));
        assertFalse(plan.scriptNames().contains("V75__productos_alta_rapida_codigos_unicos.sql"));
    }

    @Test
    void treatsErpAsAContainerModuleInsteadOfForcingTheLegacyFullPlan() {
        TenantMigrationPlan plan = planner.buildPlan(List.of("ERP"), false);

        assertFalse(plan.legacyFullMigration());
        assertFalse(plan.scriptNames().contains("V1__tenant_erp_facturacion.sql"));
        assertFalse(plan.scriptNames().contains("V2__productos_comerciales_core.sql"));
        assertFalse(plan.scriptNames().contains("V3__inventory_core.sql"));
        assertFalse(plan.scriptNames().contains("V28__crm_module.sql"));
    }

    @Test
    void buildsTheCompleteErpPlanWithLatestModuleMigrations() {
        TenantMigrationPlan plan = planner.buildPlan(List.of(
                "CAJA",
                "CLIENTES",
                "COMPRAS",
                "COTIZACIONES",
                "CRM",
                "ERP",
                "FACTURACION",
                "INVENTARIO",
                "REPORTES",
                "VENTAS"
        ), false);

        assertFalse(plan.legacyFullMigration());
        assertFalse(plan.scriptNames().contains("V1__tenant_erp_facturacion.sql"));
        assertTrue(plan.scriptNames().contains("V2__productos_comerciales_core.sql"));
        assertTrue(plan.scriptNames().contains("V2_1__clientes_core.sql"));
        assertTrue(plan.scriptNames().contains("V2_2__ventas_core.sql"));
        assertTrue(plan.scriptNames().contains("V2_3__facturacion_documental_core.sql"));
        assertTrue(plan.scriptNames().contains("V3__inventory_core.sql"));
        assertTrue(plan.scriptNames().contains("V64__paged_collection_indexes.sql"));
        assertTrue(plan.scriptNames().contains("V73__harden_public_lead_ingress.sql"));
        assertTrue(plan.scriptNames().contains("V74__audit_public_lead_rejections.sql"));
        assertTrue(plan.scriptNames().contains("V75__productos_alta_rapida_codigos_unicos.sql"));
        assertEquals(
                plan.scriptNames().size(),
                new HashSet<>(plan.scriptNames().stream().map(this::versionOf).toList()).size()
        );
    }

    @Test
    void includesQuickProductRegistrationOnlyForInventory() {
        TenantMigrationPlan plan = planner.buildPlan(List.of("INVENTARIO"), false);

        assertFalse(plan.scriptNames().contains("V1__tenant_erp_facturacion.sql"));
        assertTrue(plan.scriptNames().contains("V2__productos_comerciales_core.sql"));
        assertFalse(plan.scriptNames().contains("V2_1__clientes_core.sql"));
        assertFalse(plan.scriptNames().contains("V2_2__ventas_core.sql"));
        assertFalse(plan.scriptNames().contains("V2_3__facturacion_documental_core.sql"));
        assertFalse(plan.scriptNames().contains("V17__compras_lotes_origen_inventario.sql"));
        assertTrue(plan.scriptNames().contains("V75__productos_alta_rapida_codigos_unicos.sql"));
        assertFalse(plan.scriptNames().contains("V73__harden_public_lead_ingress.sql"));
    }

    @Test
    void keepsCrmFreeFromSalesFiscalAndInventoryMigrations() {
        TenantMigrationPlan plan = planner.buildPlan(List.of("CRM"), false);

        assertFalse(plan.scriptNames().contains("V1__tenant_erp_facturacion.sql"));
        assertTrue(plan.scriptNames().contains("V2__productos_comerciales_core.sql"));
        assertTrue(plan.scriptNames().contains("V2_1__clientes_core.sql"));
        assertFalse(plan.scriptNames().contains("V2_2__ventas_core.sql"));
        assertFalse(plan.scriptNames().contains("V2_3__facturacion_documental_core.sql"));
        assertFalse(plan.scriptNames().contains("V3__inventory_core.sql"));
        assertFalse(plan.scriptNames().contains("V5__caja_core.sql"));
        assertFalse(plan.scriptNames().contains("V48__crm_quote_product_warehouse_repair.sql"));
    }

    @Test
    void keepsSalesAndPurchasesBehindTheirOwnContracts() {
        TenantMigrationPlan inventory = planner.buildPlan(List.of("INVENTARIO"), false);
        TenantMigrationPlan sales = planner.buildPlan(List.of("VENTAS"), false);
        TenantMigrationPlan purchases = planner.buildPlan(List.of("COMPRAS"), false);

        assertFalse(inventory.scriptNames().contains("V17__compras_lotes_origen_inventario.sql"));
        assertFalse(sales.scriptNames().contains("V5__caja_core.sql"));
        assertTrue(purchases.scriptNames().contains("V17__compras_lotes_origen_inventario.sql"));
    }

    @Test
    void doesNotIncludeWhatsappNotesForTenantsWithoutCrm() {
        TenantMigrationPlan plan = planner.buildPlan(List.of("CLIENTES"), false);

        assertFalse(plan.scriptNames().contains("V71__crm_whatsapp_internal_notes.sql"));
    }

    private String versionOf(String scriptName) {
        return scriptName.substring(1, scriptName.indexOf("__"));
    }
}
