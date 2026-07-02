package com.azurion.saascore.roles.domain.repositories;

import com.azurion.saascore.roles.domain.entities.Rol;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RolRepository extends JpaRepository<Rol, Long> {

    boolean existsByCodigoIgnoreCase(String codigo);

    Optional<Rol> findByCodigoIgnoreCase(String codigo);

    @EntityGraph(attributePaths = {"rolPermisos", "rolPermisos.permiso"})
    List<Rol> findAllByOrderByNombreAsc();

    @EntityGraph(attributePaths = {"rolPermisos", "rolPermisos.permiso"})
    @Query("select r from Rol r where r.id = :id")
    Optional<Rol> findWithPermisosById(Long id);
}
