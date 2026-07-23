package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmLeadAssignmentConfig;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CrmLeadAssignmentConfigRepository extends JpaRepository<CrmLeadAssignmentConfig, Long> {

    Optional<CrmLeadAssignmentConfig> findByCodigo(String codigo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select config from CrmLeadAssignmentConfig config where config.codigo = :codigo")
    Optional<CrmLeadAssignmentConfig> findByCodigoForUpdate(@Param("codigo") String codigo);
}
