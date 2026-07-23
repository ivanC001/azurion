package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmLandingConfig;
import com.azurion.saascore.crm.domain.entities.CrmLandingCatalogItem;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrmLandingCatalogItemRepository extends JpaRepository<CrmLandingCatalogItem, Long> {

    boolean existsByLandingConfigAndCatalogoItem_IdAndActivoTrue(CrmLandingConfig landingConfig, Long catalogoItemId);

    @EntityGraph(attributePaths = "catalogoItem")
    List<CrmLandingCatalogItem> findAllByLandingConfigOrderByIdAsc(CrmLandingConfig landingConfig);
}
