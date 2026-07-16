package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmCurrencyConfig;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrmCurrencyConfigRepository extends JpaRepository<CrmCurrencyConfig, Long> {

    List<CrmCurrencyConfig> findAllByOrderByMonedaAsc();

    Optional<CrmCurrencyConfig> findByMoneda(String moneda);
}
