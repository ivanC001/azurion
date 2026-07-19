package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmOportunidadRecurso;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrmOportunidadRecursoRepository extends JpaRepository<CrmOportunidadRecurso, Long> {

    @EntityGraph(attributePaths = {"oportunidad"})
    List<CrmOportunidadRecurso> findByOportunidadIdOrderByCreatedAtDescIdDesc(Long oportunidadId);

    @EntityGraph(attributePaths = {"oportunidad"})
    List<CrmOportunidadRecurso> findAllByOrderByCreatedAtDescIdDesc();

    @EntityGraph(attributePaths = {"oportunidad"})
    List<CrmOportunidadRecurso> findByOportunidadResponsableIdOrderByCreatedAtDescIdDesc(String responsableId);

    @EntityGraph(attributePaths = {"oportunidad"})
    Optional<CrmOportunidadRecurso> findWithOportunidadById(Long id);

    @EntityGraph(attributePaths = {"oportunidad"})
    Optional<CrmOportunidadRecurso> findByOportunidadIdAndTipoAndExternalKey(
            Long oportunidadId,
            String tipo,
            String externalKey
    );
}
