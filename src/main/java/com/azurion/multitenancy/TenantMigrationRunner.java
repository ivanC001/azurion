package com.azurion.multitenancy;

import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TenantMigrationRunner {

    private final TenantSchemaRegistryRepository registryRepository;
    private final TenantMigrationService migrationService;

    @PostConstruct
    public void migrateExistingTenants() {
        List<TenantSchemaRegistry> registries = registryRepository.findAll();
        migrationService.migrateSchemas(registries);
    }
}
