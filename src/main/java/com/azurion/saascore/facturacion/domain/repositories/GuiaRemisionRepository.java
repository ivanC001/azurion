package com.azurion.saascore.facturacion.domain.repositories;

import com.azurion.saascore.facturacion.domain.entities.GuiaRemision;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuiaRemisionRepository extends JpaRepository<GuiaRemision, Long> {
    Optional<GuiaRemision> findByExternalId(String externalId);
}
