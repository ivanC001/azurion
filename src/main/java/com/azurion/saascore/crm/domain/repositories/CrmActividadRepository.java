package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmActividad;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CrmActividadRepository extends JpaRepository<CrmActividad, Long>, JpaSpecificationExecutor<CrmActividad> {

    void deleteByProspecto_Id(Long prospectoId);

    @EntityGraph(attributePaths = {"prospecto", "oportunidad", "cliente"})
    List<CrmActividad> findAllByOrderByFechaProgramadaAscIdDesc();

    @EntityGraph(attributePaths = {"prospecto", "oportunidad", "cliente"})
    List<CrmActividad> findByUsuarioIdOrderByFechaProgramadaAscIdDesc(String usuarioId);

    @EntityGraph(attributePaths = {"prospecto", "oportunidad", "cliente"})
    List<CrmActividad> findByUsuarioIdInOrderByFechaProgramadaAscIdDesc(List<String> usuarioIds);

    @EntityGraph(attributePaths = {"prospecto", "oportunidad", "cliente"})
    List<CrmActividad> findByOportunidadIdOrderByFechaProgramadaAscIdDesc(Long oportunidadId);

    long countByEstado(String estado);

    long countByUsuarioIdAndEstado(String usuarioId, String estado);

    long countByEstadoAndFechaProgramadaBefore(String estado, OffsetDateTime fechaProgramada);

    long countByUsuarioIdAndEstadoAndFechaProgramadaBefore(String usuarioId, String estado, OffsetDateTime fechaProgramada);

    @Override
    @EntityGraph(attributePaths = {"prospecto", "oportunidad", "cliente"})
    Page<CrmActividad> findAll(Specification<CrmActividad> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"prospecto", "oportunidad", "cliente"})
    @Query("""
            select a
            from CrmActividad a
            left join a.prospecto p
            left join a.oportunidad o
            left join a.cliente c
            where (:scopeAll = true or a.usuarioId in :usuarioScope)
              and (:query is null
                   or lower(coalesce(a.asunto, '')) like concat('%', :query, '%')
                   or lower(coalesce(a.descripcion, '')) like concat('%', :query, '%')
                   or lower(coalesce(p.nombre, '')) like concat('%', :query, '%')
                   or lower(coalesce(p.telefono, '')) like concat('%', :query, '%')
                   or lower(coalesce(p.correo, '')) like concat('%', :query, '%')
                   or lower(coalesce(o.titulo, '')) like concat('%', :query, '%')
                   or lower(coalesce(c.nombre, '')) like concat('%', :query, '%')
                   or lower(coalesce(c.email, '')) like concat('%', :query, '%')
                   or lower(coalesce(c.telefono, '')) like concat('%', :query, '%')
                   or lower(coalesce(c.numeroDocumento, '')) like concat('%', :query, '%'))
              and (:estado is null or a.estado = :estado)
              and (:tipoActividad is null or a.tipoActividad = :tipoActividad)
              and (:usuarioId is null or a.usuarioId = :usuarioId)
              and (:prospectoId is null or p.id = :prospectoId)
              and (:oportunidadId is null or o.id = :oportunidadId)
              and (:fechaDesde is null or a.fechaProgramada >= :fechaDesde)
              and (:fechaHasta is null or a.fechaProgramada <= :fechaHasta)
            """)
    Page<CrmActividad> searchPage(@Param("scopeAll") boolean scopeAll,
                                  @Param("usuarioScope") List<String> usuarioScope,
                                  @Param("query") String query,
                                  @Param("estado") String estado,
                                  @Param("tipoActividad") String tipoActividad,
                                  @Param("usuarioId") String usuarioId,
                                  @Param("prospectoId") Long prospectoId,
                                  @Param("oportunidadId") Long oportunidadId,
                                  @Param("fechaDesde") OffsetDateTime fechaDesde,
                                  @Param("fechaHasta") OffsetDateTime fechaHasta,
                                  Pageable pageable);
}
