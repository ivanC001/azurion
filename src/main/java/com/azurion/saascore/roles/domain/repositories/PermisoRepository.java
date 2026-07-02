package com.azurion.saascore.roles.domain.repositories;

import com.azurion.saascore.roles.domain.entities.Permiso;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermisoRepository extends JpaRepository<Permiso, Long> {

    boolean existsByCodigoIgnoreCase(String codigo);

    Optional<Permiso> findByCodigoIgnoreCase(String codigo);

    List<Permiso> findAllByOrderByModuloAscNombreAsc();

    List<Permiso> findAllByActivoTrueOrderByModuloAscNombreAsc();

    List<Permiso> findByIdIn(Collection<Long> ids);
}
