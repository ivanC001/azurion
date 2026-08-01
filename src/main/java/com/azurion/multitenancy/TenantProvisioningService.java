package com.azurion.multitenancy;

import com.azurion.shared.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class TenantProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(TenantProvisioningService.class);
    private final TenantSchemaRegistryRepository registryRepository;
    private final TenantMigrationService tenantMigrationService;
    private final JdbcTemplate jdbcTemplate;

    public void createTenantSchema(String tenantId, String schemaName) {
        createTenantSchema(tenantId, schemaName, null);
    }

    public void createTenantSchema(String tenantId, String schemaName, List<String> moduloCodigos) {
        validateIdentifier("tenantId", tenantId);
        validateIdentifier("schemaName", schemaName);

        registryRepository.findByTenantId(tenantId).ifPresent(existing -> {
            throw new BusinessException("TENANT_EXISTS", "Tenant already registered: " + tenantId);
        });
        registryRepository.findBySchemaName(schemaName).ifPresent(existing -> {
            throw new BusinessException("TENANT_SCHEMA_EXISTS", "Schema already registered: " + schemaName);
        });

        boolean existingSchema = schemaExists(schemaName);
        if (existingSchema) {
            if (schemaContainsTables(schemaName) && !schemaHasFlywayHistory(schemaName)) {
                throw new BusinessException(
                        "TENANT_SCHEMA_UNMANAGED",
                        "El schema existente no tiene un historial Flyway valido y no puede adoptarse: " + schemaName
                );
            }
            log.warn(
                    "Existing tenant schema detected; validating and applying pending migrations tenantId={} schema={}",
                    tenantId,
                    schemaName
            );
        } else {
            log.info("Creating tenant schema tenantId={} schema={}", tenantId, schemaName);
            registerRollbackCleanup(schemaName);
        }

        try {
            tenantMigrationService.migrateSchema(schemaName, moduloCodigos, moduloCodigos == null);
        } catch (RuntimeException exception) {
            if (!existingSchema && !TransactionSynchronizationManager.isActualTransactionActive()) {
                dropSchemaQuietly(schemaName);
            }
            throw exception;
        }

        TenantSchemaRegistry registry = new TenantSchemaRegistry();
        registry.setTenantId(tenantId);
        registry.setSchemaName(schemaName);
        registry.setActive(true);
        registryRepository.save(registry);
    }

    private void validateIdentifier(String field, String value) {
        if (value == null || !value.matches("^[a-z][a-z0-9_]{2,62}$")) {
            throw new BusinessException("INVALID_" + field.toUpperCase(),
                    field + " must match ^[a-z][a-z0-9_]{2,62}$");
        }
    }

    private boolean schemaExists(String schemaName) {
        Boolean exists = jdbcTemplate.queryForObject(
                "select exists (select 1 from information_schema.schemata where schema_name = ?)",
                Boolean.class,
                schemaName
        );
        return Boolean.TRUE.equals(exists);
    }

    private boolean schemaContainsTables(String schemaName) {
        Boolean containsTables = jdbcTemplate.queryForObject(
                "select exists (select 1 from information_schema.tables where table_schema = ?)",
                Boolean.class,
                schemaName
        );
        return Boolean.TRUE.equals(containsTables);
    }

    private boolean schemaHasFlywayHistory(String schemaName) {
        Boolean hasHistory = jdbcTemplate.queryForObject(
                "select exists (select 1 from information_schema.tables where table_schema = ? and table_name = 'flyway_schema_history')",
                Boolean.class,
                schemaName
        );
        return Boolean.TRUE.equals(hasHistory);
    }

    private void registerRollbackCleanup(String schemaName) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    dropSchemaQuietly(schemaName);
                }
            }
        });
    }

    private void dropSchemaQuietly(String schemaName) {
        try {
            jdbcTemplate.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
            log.warn("Removed tenant schema after failed provisioning schema={}", schemaName);
        } catch (RuntimeException cleanupError) {
            log.error("Could not remove tenant schema after failed provisioning schema={}", schemaName, cleanupError);
        }
    }
}
