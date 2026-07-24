package com.azurion.saascore.settings.email.domain.repositories;

import com.azurion.saascore.settings.email.domain.entities.PlatformEmailConfig;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformEmailConfigRepository extends JpaRepository<PlatformEmailConfig, Long> {

    Optional<PlatformEmailConfig> findByConfigKey(String configKey);
}
