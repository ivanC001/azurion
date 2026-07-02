package com.azurion.multitenancy;

import com.azurion.shared.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

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

        if (schemaExists(schemaName)) {
            log.warn(
                    "Adopting existing tenant schema without running migrations tenantId={} schema={}",
                    tenantId,
                    schemaName
            );
        } else {
            log.info("Creating tenant schema tenantId={} schema={}", tenantId, schemaName);
            tenantMigrationService.migrateSchema(schemaName, moduloCodigos, moduloCodigos == null);
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
}
