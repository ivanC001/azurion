package com.azurion.saascore.planes.domain.repositories;

import com.azurion.saascore.planes.domain.entities.PlanModulo;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PlanModuloRepository extends JpaRepository<PlanModulo, Long> {

    @EntityGraph(attributePaths = {"modulo"})
    List<PlanModulo> findByPlanIdOrderByModuloNombreAsc(Long planId);

    void deleteByPlanId(Long planId);

    @Query("""
            select pm.modulo.codigo
            from PlanModulo pm
            where pm.plan.id = :planId
            order by pm.modulo.codigo
            """)
    List<String> findModuloCodigosByPlanId(Long planId);
}
