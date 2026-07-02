package com.azurion.saascore.inventory.domain.repositories;

import com.azurion.saascore.inventory.domain.entities.UnidadMedida;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnidadMedidaRepository extends JpaRepository<UnidadMedida, Long> {
    Optional<UnidadMedida> findByCodigoSunat(String codigoSunat);
}
