package com.azurion.saascore.modulos.domain.repositories;

import com.azurion.saascore.modulos.domain.entities.Modulo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModuloRepository extends JpaRepository<Modulo, Long> {

    Optional<Modulo> findByCodigoIgnoreCase(String codigo);

    List<Modulo> findAllByOrderByNombreAsc();
}
