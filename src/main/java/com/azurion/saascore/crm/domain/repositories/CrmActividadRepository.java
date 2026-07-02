package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmActividad;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrmActividadRepository extends JpaRepository<CrmActividad, Long> {

    @EntityGraph(attributePaths = {"prospecto", "oportunidad", "cliente"})
    List<CrmActividad> findAllByOrderByFechaProgramadaAscIdDesc();

    @EntityGraph(attributePaths = {"prospecto", "oportunidad", "cliente"})
    List<CrmActividad> findByUsuarioIdOrderByFechaProgramadaAscIdDesc(String usuarioId);

    @EntityGraph(attributePaths = {"prospecto", "oportunidad", "cliente"})
    List<CrmActividad> findByOportunidadIdOrderByFechaProgramadaAscIdDesc(Long oportunidadId);

    long countByEstado(String estado);

    long countByUsuarioIdAndEstado(String usuarioId, String estado);

    long countByEstadoAndFechaProgramadaBefore(String estado, OffsetDateTime fechaProgramada);

    long countByUsuarioIdAndEstadoAndFechaProgramadaBefore(String usuarioId, String estado, OffsetDateTime fechaProgramada);
}
