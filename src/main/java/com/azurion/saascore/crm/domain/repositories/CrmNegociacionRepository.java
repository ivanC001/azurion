package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmNegociacion;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrmNegociacionRepository extends JpaRepository<CrmNegociacion, Long> {

    List<CrmNegociacion> findByOportunidadIdOrderByCreatedAtDescIdDesc(Long oportunidadId);

    Optional<CrmNegociacion> findFirstByOportunidadIdOrderByCreatedAtDescIdDesc(Long oportunidadId);

    boolean existsByOportunidadIdAndEstadoIn(Long oportunidadId, Collection<String> estados);
}
