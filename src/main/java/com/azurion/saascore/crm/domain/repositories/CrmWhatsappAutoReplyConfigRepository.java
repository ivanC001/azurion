package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmWhatsappAutoReplyConfig;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrmWhatsappAutoReplyConfigRepository extends JpaRepository<CrmWhatsappAutoReplyConfig, Long> {
    Optional<CrmWhatsappAutoReplyConfig> findFirstByOrderByIdAsc();
}
