package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmCatalogoItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrmCatalogoItemRepository extends JpaRepository<CrmCatalogoItem, Long> {

    List<CrmCatalogoItem> findAllByOrderByIdDesc();

    List<CrmCatalogoItem> findByTipoItemOrderByIdDesc(String tipoItem);

    Optional<CrmCatalogoItem> findByIdAndPublicToken(Long id, String publicToken);

    boolean existsByPublicToken(String publicToken);
}
