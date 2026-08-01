package com.azurion.multitenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.azurion.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

class TenantMigrationReadinessTest {

    private final TenantMigrationReadiness readiness = new TenantMigrationReadiness();

    @Test
    void isolatesOnlyTheTenantWhoseMigrationFailed() {
        readiness.markFailed("tenant_broken", new IllegalStateException("migration failed"));

        readiness.requireReady("tenant_healthy");

        assertThatThrownBy(() -> readiness.requireReady("tenant_broken"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("TENANT_SCHEMA_UNAVAILABLE");
                    assertThat(exception.getStatus().value()).isEqualTo(503);
                });
    }

    @Test
    void allowsTrafficAgainAfterSuccessfulRetry() {
        readiness.markFailed("tenant_demo", new IllegalStateException("migration failed"));
        readiness.markReady("tenant_demo");

        readiness.requireReady("tenant_demo");
        assertThat(readiness.failures()).doesNotContainKey("tenant_demo");
    }
}
