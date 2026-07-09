package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmCanalTokenConfig;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrmCanalTokenConfigRepository extends JpaRepository<CrmCanalTokenConfig, Long> {

    List<CrmCanalTokenConfig> findAllByOrderByCanalAsc();

    Optional<CrmCanalTokenConfig> findByCanal(String canal);
}
