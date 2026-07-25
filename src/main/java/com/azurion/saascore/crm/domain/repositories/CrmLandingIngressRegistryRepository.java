package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmLandingIngressRegistry;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrmLandingIngressRegistryRepository extends JpaRepository<CrmLandingIngressRegistry, Long> {

    Optional<CrmLandingIngressRegistry> findBySourceKeyAndActivoTrue(String sourceKey);

    Optional<CrmLandingIngressRegistry> findByTenantIdAndLandingConfigId(String tenantId, Long landingConfigId);
}
