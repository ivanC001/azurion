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
    }

    @Test
    void includesWhatsappConnectionStatusInTheFullTenantPlan() {
        TenantMigrationPlan plan = planner.buildPlan(List.of("ERP"), false);

        assertTrue(plan.scriptNames().contains("V62__crm_whatsapp_connection_status.sql"));
        assertTrue(plan.scriptNames().contains("V64__paged_collection_indexes.sql"));
        assertTrue(plan.scriptNames().contains("V65__crm_whatsapp_delivery_status.sql"));
        assertTrue(plan.scriptNames().contains("V67__activate_default_tenant_admin.sql"));
        assertTrue(plan.scriptNames().contains("V68__crm_landing_optional_product_default.sql"));
        assertTrue(plan.scriptNames().contains("V69__crm_sent_email_inbox_index.sql"));
        assertTrue(plan.scriptNames().contains("V70__reconcile_default_tenant_admin.sql"));
        assertTrue(plan.scriptNames().contains("V71__crm_whatsapp_internal_notes.sql"));
        assertEquals(
                plan.scriptNames().size(),
                new HashSet<>(plan.scriptNames().stream().map(this::versionOf).toList()).size()
        );
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
