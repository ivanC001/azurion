package com.azurion.multitenancy;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/** Exposes schema drift as an alertable metric without failing healthy tenants. */
@Component
public class TenantMigrationMetrics {

    public TenantMigrationMetrics(MeterRegistry registry, TenantMigrationReadiness readiness) {
        Gauge.builder(
                        "azurion.tenant.migration.failures",
                        readiness,
                        state -> state.failures().size()
                )
                .description("Number of tenant schemas blocked after a migration failure")
                .register(registry);
    }
}
