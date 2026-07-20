package com.azurion.multitenancy;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
    }

    @Test
    void includesWhatsappConnectionStatusInTheFullTenantPlan() {
        TenantMigrationPlan plan = planner.buildPlan(List.of("ERP"), false);

        assertTrue(plan.scriptNames().contains("V62__crm_whatsapp_connection_status.sql"));
        assertTrue(plan.scriptNames().contains("V64__paged_collection_indexes.sql"));
        assertTrue(plan.scriptNames().contains("V65__crm_whatsapp_delivery_status.sql"));
    }
}
