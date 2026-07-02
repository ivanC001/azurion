package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmEtapaPipeline;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrmEtapaPipelineRepository extends JpaRepository<CrmEtapaPipeline, Long> {

    List<CrmEtapaPipeline> findByActivoTrueOrderByOrdenAscIdAsc();

    Optional<CrmEtapaPipeline> findByCodigo(String codigo);

    Optional<CrmEtapaPipeline> findByIdAndActivoTrue(Long id);

    boolean existsByCodigo(String codigo);
}
