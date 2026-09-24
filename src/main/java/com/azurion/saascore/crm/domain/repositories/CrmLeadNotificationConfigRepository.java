package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmLeadNotificationConfig;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrmLeadNotificationConfigRepository extends JpaRepository<CrmLeadNotificationConfig, Long> {

    Optional<CrmLeadNotificationConfig> findFirstByOrderByIdAsc();
}
