package com.azurion.saascore.settings.email.domain.repositories;

import com.azurion.saascore.settings.email.domain.entities.TenantEmailConfig;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantEmailConfigRepository extends JpaRepository<TenantEmailConfig, Long> {

    Optional<TenantEmailConfig> findByTenantId(String tenantId);
}
