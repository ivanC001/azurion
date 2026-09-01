package com.azurion.multitenancy;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class TenantModuleMigrationPlannerTest {

    private final TenantModuleMigrationPlanner planner = new TenantModuleMigrationPlanner();

    /** Modulos contratables por un tenant, tal y como los expone el planner. */
    private static final List<String> MODULES = List.of(
            "INVENTARIO", "COMPRAS", "CLIENTES", "VENTAS",
            "CAJA", "FACTURACION", "COTIZACIONES", "CRM"
    );

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
        assertTrue(plan.scriptNames().contains("V78__cotizacion_whatsapp_send_guard.sql"));
        assertTrue(plan.scriptNames().contains("V79__crm_open_opportunity_next_action.sql"));
        assertTrue(plan.scriptNames().contains("V80__crm_prospect_person_classification.sql"));
        assertTrue(plan.scriptNames().contains("V81__crm_prospect_country_identification.sql"));
        assertFalse(plan.scriptNames().contains("V86__reconcile_erp_tax_permissions.sql"));
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
        assertFalse(plan.scriptNames().contains("V86__reconcile_erp_tax_permissions.sql"));
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
        assertTrue(plan.scriptNames().contains("V76__inventory_consistency_rules.sql"));
        assertTrue(plan.scriptNames().contains("V77__caja_turnos_refactoring.sql"));
        assertTrue(plan.scriptNames().contains("V78__cotizacion_whatsapp_send_guard.sql"));
        assertTrue(plan.scriptNames().contains("V79__crm_open_opportunity_next_action.sql"));
        assertTrue(plan.scriptNames().contains("V80__crm_prospect_person_classification.sql"));
        assertTrue(plan.scriptNames().contains("V81__crm_prospect_country_identification.sql"));
        assertTrue(plan.scriptNames().contains("V86__reconcile_erp_tax_permissions.sql"));
        assertEquals(
                plan.scriptNames().size(),
                new HashSet<>(plan.scriptNames().stream().map(this::versionOf).toList()).size()
        );
    }

    @Test
    void registersEveryTenantMigrationInTheLegacyPlan() throws IOException {
        TenantMigrationPlan plan = planner.buildPlan(List.of(), true);
        HashSet<String> registeredScripts = new HashSet<>(plan.scriptNames());
        Resource[] migrationResources = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:db/migration/tenant/V*.sql");

        List<String> unregisteredScripts = Arrays.stream(migrationResources)
                .map(Resource::getFilename)
                .filter(filename -> filename != null && !registeredScripts.contains(filename))
                .sorted()
                .toList();

        assertTrue(
                unregisteredScripts.isEmpty(),
                () -> "Unregistered tenant migrations: " + unregisteredScripts
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
        assertTrue(plan.scriptNames().contains("V76__inventory_consistency_rules.sql"));
        assertFalse(plan.scriptNames().contains("V77__caja_turnos_refactoring.sql"));
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
    void includesShiftRefactoringOnlyForCashModule() {
        TenantMigrationPlan cash = planner.buildPlan(List.of("CAJA"), false);
        TenantMigrationPlan sales = planner.buildPlan(List.of("VENTAS"), false);

        assertTrue(cash.scriptNames().contains("V77__caja_turnos_refactoring.sql"));
        assertTrue(cash.scriptNames().contains("V2_2__ventas_core.sql"));
        assertFalse(sales.scriptNames().contains("V77__caja_turnos_refactoring.sql"));
    }

    @Test
    void doesNotIncludeWhatsappNotesForTenantsWithoutCrm() {
        TenantMigrationPlan plan = planner.buildPlan(List.of("CLIENTES"), false);

        assertFalse(plan.scriptNames().contains("V71__crm_whatsapp_internal_notes.sql"));
    }

    /**
     * Un tenant que cotiza sin INVENTARIO recibe la tabla productos (V2) pero no
     * las migraciones que le anaden almacen_id (V8) ni precio_venta_modo (V85),
     * que solo estan en el plan de INVENTARIO.
     *
     * La entidad Producto mapea ambas columnas y CotizacionRepository la carga
     * en cada lectura, asi que sin V98 toda consulta de cotizaciones falla con
     * "column p1_0.almacen_id does not exist".
     */
    @Test
    void alignsProductColumnsForQuotingTenantsWithoutInventory() {
        TenantMigrationPlan cotizaciones = planner.buildPlan(List.of("COTIZACIONES"), false);
        TenantMigrationPlan crm = planner.buildPlan(List.of("CRM"), false);

        assertTrue(cotizaciones.scriptNames().contains("V2__productos_comerciales_core.sql"));
        assertFalse(cotizaciones.scriptNames().contains("V8__productos_require_almacen.sql"));
        assertFalse(cotizaciones.scriptNames().contains("V85__professional_tax_and_margin_model.sql"));

        assertTrue(cotizaciones.scriptNames().contains("V98__cotizaciones_productos_schema_alignment.sql"));
        assertTrue(crm.scriptNames().contains("V98__cotizaciones_productos_schema_alignment.sql"));
    }

    /**
     * Quien si tiene INVENTARIO recibe las columnas por su via original; la
     * migracion de alineacion es idempotente y no debe estorbar.
     */
    @Test
    void inventoryTenantsKeepTheirOriginalProductMigrations() {
        TenantMigrationPlan plan = planner.buildPlan(List.of("INVENTARIO"), false);

        assertTrue(plan.scriptNames().contains("V8__productos_require_almacen.sql"));
        assertTrue(plan.scriptNames().contains("V85__professional_tax_and_margin_model.sql"));
    }

    /**
     * Invariante: todo modulo que cree o modifique `sucursales` tiene que
     * arrastrar tambien la migracion que hace opcional el ubigeo.
     *
     * V15 (ERP) y V43 (CRM) dejan `ubigeo_codigo` en NOT NULL. Desde que
     * SucursalLocationResolver permite sucursales sin ubigeo fuera de Peru, un
     * plan que incluya una de esas dos sin V99 rompe con una violacion de NOT
     * NULL en cuanto el tenant extranjero registra una sede.
     *
     * Se comprueba modulo a modulo porque el hueco original era exactamente
     * ese: V99 solo estaba en COTIZACIONES, asi que cualquier tenant de ERP
     * (INVENTARIO, VENTAS, CAJA...) se quedaba sin ella.
     */
    @Test
    void everyModuleThatTouchesBranchesRelaxesTheUbigeoConstraint() {
        for (String module : MODULES) {
            List<String> scripts = planner.buildPlan(List.of(module), false).scriptNames();

            boolean definesBranchUbigeo = scripts.contains("V15__sucursales_ubigeo_igv.sql")
                    || scripts.contains("V43__crm_default_branch_support.sql");

            if (definesBranchUbigeo) {
                assertTrue(
                        scripts.contains("V99__sucursal_ubigeo_optional_outside_peru.sql"),
                        () -> "El modulo " + module + " deja sucursales.ubigeo_codigo NOT NULL"
                                + " y no incluye V99"
                );
            }
        }
    }

    /**
     * Invariante hermano: la entidad Producto mapea `almacen_id` y
     * `precio_venta_modo`, asi que cualquier plan con la tabla `productos`
     * tiene que poder responder por esas dos columnas.
     *
     * Las anaden V8 (solo INVENTARIO) y V85. Un plan con V2 pero sin ellas
     * necesita V98, que las crea de forma idempotente; sin eso todo LEFT JOIN
     * sobre productos falla con "column p1_0.almacen_id does not exist".
     */
    @Test
    void everyModuleWithProductsCanSatisfyTheProductEntityMapping() {
        for (String module : MODULES) {
            List<String> scripts = planner.buildPlan(List.of(module), false).scriptNames();

            if (!scripts.contains("V2__productos_comerciales_core.sql")) {
                continue;
            }

            boolean hasAlmacenId = scripts.contains("V8__productos_require_almacen.sql")
                    || scripts.contains("V98__cotizaciones_productos_schema_alignment.sql");
            boolean hasPrecioVentaModo =
                    scripts.contains("V85__professional_tax_and_margin_model.sql")
                            || scripts.contains("V98__cotizaciones_productos_schema_alignment.sql");

            assertTrue(hasAlmacenId, () -> "El modulo " + module + " tiene productos sin almacen_id");
            assertTrue(
                    hasPrecioVentaModo,
                    () -> "El modulo " + module + " tiene productos sin precio_venta_modo"
            );
        }
    }

    private String versionOf(String scriptName) {
        return scriptName.substring(1, scriptName.indexOf("__"));
    }
}
