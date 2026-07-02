package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmOportunidad;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrmOportunidadRepository extends JpaRepository<CrmOportunidad, Long> {

    @EntityGraph(attributePaths = {"prospecto", "cliente", "etapaPipeline"})
    List<CrmOportunidad> findAllByOrderByIdDesc();

    @EntityGraph(attributePaths = {"prospecto", "cliente", "etapaPipeline"})
    List<CrmOportunidad> findByResponsableIdOrderByIdDesc(String responsableId);

    @EntityGraph(attributePaths = {"prospecto", "cliente", "etapaPipeline"})
    Optional<CrmOportunidad> findFirstByProspectoIdAndEstadoOrderByIdDesc(Long prospectoId, String estado);

    long countByEstado(String estado);

    long countByResponsableIdAndEstado(String responsableId, String estado);
}
