package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmLandingConfig;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrmLandingConfigRepository extends JpaRepository<CrmLandingConfig, Long> {

    Optional<CrmLandingConfig> findByLandingKey(String landingKey);

    boolean existsByLandingKey(String landingKey);

    List<CrmLandingConfig> findAllByOrderByCreatedAtDesc();
}
