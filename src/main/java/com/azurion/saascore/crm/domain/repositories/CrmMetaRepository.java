package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmMeta;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrmMetaRepository extends JpaRepository<CrmMeta, Long> {

    List<CrmMeta> findByAnioAndMesOrderByAlcanceAscResponsableIdAsc(Integer anio, Integer mes);

    Optional<CrmMeta> findByAnioAndMesAndAlcanceAndResponsableId(
            Integer anio,
            Integer mes,
            String alcance,
            String responsableId
    );

    Optional<CrmMeta> findByAnioAndMesAndAlcanceAndResponsableIdIsNull(
            Integer anio,
            Integer mes,
            String alcance
    );
}
