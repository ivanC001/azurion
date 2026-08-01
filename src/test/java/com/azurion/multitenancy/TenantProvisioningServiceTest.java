package com.azurion.multitenancy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.shared.exception.BusinessException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class TenantProvisioningServiceTest {

    @Test
    void existingSchemaIsMigratedBeforeItIsRegistered() {
        TenantSchemaRegistryRepository registryRepository = mock(TenantSchemaRegistryRepository.class);
        TenantMigrationService migrationService = mock(TenantMigrationService.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(registryRepository.findByTenantId("tenant_demo")).thenReturn(Optional.empty());
        when(registryRepository.findBySchemaName("tenant_demo_schema")).thenReturn(Optional.empty());
        when(jdbcTemplate.queryForObject(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(Boolean.class),
                org.mockito.ArgumentMatchers.<Object>any()
        )).thenReturn(true);
        TenantProvisioningService service = new TenantProvisioningService(
                registryRepository,
                migrationService,
                jdbcTemplate
        );

        service.createTenantSchema("tenant_demo", "tenant_demo_schema", List.of("ERP"));

        verify(migrationService).migrateSchema("tenant_demo_schema", List.of("ERP"), false);
        verify(registryRepository).save(org.mockito.ArgumentMatchers.argThat(registry ->
                "tenant_demo".equals(registry.getTenantId())
                        && "tenant_demo_schema".equals(registry.getSchemaName())
                        && registry.isActive()
        ));
    }

    @Test
    void unmanagedExistingSchemaIsRejected() {
        TenantSchemaRegistryRepository registryRepository = mock(TenantSchemaRegistryRepository.class);
        TenantMigrationService migrationService = mock(TenantMigrationService.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(registryRepository.findByTenantId("tenant_demo")).thenReturn(Optional.empty());
        when(registryRepository.findBySchemaName("tenant_demo_schema")).thenReturn(Optional.empty());
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any()))
                .thenReturn(true, true, false);

        TenantProvisioningService service = new TenantProvisioningService(
                registryRepository,
                migrationService,
                jdbcTemplate
        );

        assertThatThrownBy(() -> service.createTenantSchema(
                "tenant_demo",
                "tenant_demo_schema",
                List.of("ERP")
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("historial Flyway");

        verify(migrationService, never()).migrateSchema(anyString(), any(), eq(false));
        verify(registryRepository, never()).save(any());
    }
}
