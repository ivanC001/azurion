package com.azurion.saascore.roles.domain.repositories;

import com.azurion.saascore.roles.domain.entities.RolPermiso;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolPermisoRepository extends JpaRepository<RolPermiso, Long> {

    boolean existsByRol_IdAndPermiso_Id(Long rolId, Long permisoId);

    long countByPermiso_Id(Long permisoId);

    @EntityGraph(attributePaths = {"permiso"})
    List<RolPermiso> findByRol_IdOrderByPermiso_ModuloAscPermiso_NombreAsc(Long rolId);

    void deleteByRol_IdAndPermiso_Id(Long rolId, Long permisoId);
}
