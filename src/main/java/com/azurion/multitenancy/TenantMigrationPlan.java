package com.azurion.multitenancy;

import java.util.List;

public record TenantMigrationPlan(
        boolean legacyFullMigration,
        List<String> moduleCodes,
        List<String> scriptNames
) {
}
