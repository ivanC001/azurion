package com.azurion.saascore.auth.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PermissionModulePolicyTest {

    private final PermissionModulePolicy policy = new PermissionModulePolicy();

    @ParameterizedTest(name = "{0}: permiso {1}, modulos {2} => {3}")
    @CsvSource({
            "ADMIN_EMPRESA,SEGURIDAD,'',true",
            "ERP_ADMIN,VENTAS,'ERP|VENTAS',true",
            "ERP_VENDEDOR,VENTAS,'ERP|VENTAS',true",
            "ERP_CAJERO,CAJA,'ERP|CAJA|VENTAS',true",
            "ERP_ALMACENERO,INVENTORY,'ERP|INVENTARIO',true",
            "ERP_CONTADOR,TRIBUTACION,'ERP|FACTURACION',true",
            "CRM_ADMIN,CRM,'CRM',true",
            "CRM_GERENTE,CRM,'CRM',true",
            "CRM_SUPERVISOR,CRM,'CRM',true",
            "CRM_VENDEDOR,CRM,'CRM',true",
            "CRM_MARKETING,CRM,'CRM',true",
            "CRM_CALLCENTER,CRM,'CRM',true",
            "CRM_VENDEDOR,CRM,'ERP|VENTAS',false",
            "ERP_VENDEDOR,VENTAS,'VENTAS',false",
            "ERP_VENDEDOR,VENTAS,'CRM|CLIENTES|COTIZACIONES',false",
            "CRM_ADMIN,INVENTORY,'CRM|CLIENTES|COTIZACIONES',false",
            "SHARED,CLIENTES,'CLIENTES|COTIZACIONES',true",
            "SHARED,COTIZACIONES,'CRM|CLIENTES',false"
    })
    void shouldAlignPermissionsWithContractedModules(
            String role,
            String permissionModule,
            String activeModuleValue,
            boolean expected
    ) {
        List<String> activeModules = activeModuleValue.isBlank()
                ? List.of()
                : Arrays.asList(activeModuleValue.split("\\|"));

        assertEquals(expected, policy.isAllowed(permissionModule, activeModules), role);
    }
}
