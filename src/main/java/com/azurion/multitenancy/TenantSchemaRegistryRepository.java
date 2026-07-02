package com.azurion.multitenancy;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantSchemaRegistryRepository extends JpaRepository<TenantSchemaRegistry, Long> {
    Optional<TenantSchemaRegistry> findByTenantId(String tenantId);

    Optional<TenantSchemaRegistry> findByTenantIdAndActiveTrue(String tenantId);

    Optional<TenantSchemaRegistry> findBySchemaName(String schemaName);
}
