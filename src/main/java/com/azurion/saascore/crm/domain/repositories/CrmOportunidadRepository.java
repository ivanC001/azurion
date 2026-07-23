package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmOportunidad;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CrmOportunidadRepository extends JpaRepository<CrmOportunidad, Long>, JpaSpecificationExecutor<CrmOportunidad> {

    boolean existsByProspecto_Id(Long prospectoId);

    @EntityGraph(attributePaths = {"prospecto", "cliente", "etapaPipeline"})
    List<CrmOportunidad> findAllByOrderByIdDesc();

    @EntityGraph(attributePaths = {"prospecto", "cliente", "etapaPipeline"})
    List<CrmOportunidad> findByResponsableIdOrderByIdDesc(String responsableId);

    @EntityGraph(attributePaths = {"prospecto", "cliente", "etapaPipeline"})
    Optional<CrmOportunidad> findFirstByProspectoIdAndEstadoOrderByIdDesc(Long prospectoId, String estado);

    @EntityGraph(attributePaths = {"prospecto", "cliente", "etapaPipeline"})
    Optional<CrmOportunidad> findWithRelationsById(Long id);

    long countByEstado(String estado);

    long countByResponsableIdAndEstado(String responsableId, String estado);

    @Override
    @EntityGraph(attributePaths = {"prospecto", "cliente", "etapaPipeline"})
    Page<CrmOportunidad> findAll(Specification<CrmOportunidad> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"prospecto", "cliente", "etapaPipeline"})
    @Query("""
            select o
            from CrmOportunidad o
            left join o.prospecto p
            left join o.cliente c
            left join o.etapaPipeline e
            where (:scopeAll = true or o.responsableId in :responsableScope)
              and (:query is null
                   or lower(coalesce(o.titulo, '')) like concat('%', :query, '%')
                   or lower(coalesce(o.descripcion, '')) like concat('%', :query, '%')
                   or lower(coalesce(p.nombre, '')) like concat('%', :query, '%')
                   or lower(coalesce(p.telefono, '')) like concat('%', :query, '%')
                   or lower(coalesce(p.correo, '')) like concat('%', :query, '%')
                   or lower(coalesce(c.nombre, '')) like concat('%', :query, '%')
                   or lower(coalesce(c.email, '')) like concat('%', :query, '%')
                   or lower(coalesce(c.telefono, '')) like concat('%', :query, '%')
                   or lower(coalesce(c.numeroDocumento, '')) like concat('%', :query, '%'))
              and (:etapaId is null or e.id = :etapaId)
              and (:etapa is null or o.etapa = :etapa)
              and (:estado is null or o.estado = :estado)
              and (:responsableId is null or o.responsableId = :responsableId)
              and (:cierreDesde is null or o.fechaCierreEstimada >= :cierreDesde)
              and (:cierreHasta is null or o.fechaCierreEstimada <= :cierreHasta)
              and (:soloPagosPendientes = false
                   or (o.estado = 'GANADA'
                       and (o.montoReal is null or o.montoReal < o.montoEstimado)))
            """)
    Page<CrmOportunidad> searchPage(@Param("scopeAll") boolean scopeAll,
                                    @Param("responsableScope") List<String> responsableScope,
                                    @Param("query") String query,
                                    @Param("etapaId") Long etapaId,
                                    @Param("etapa") String etapa,
                                    @Param("estado") String estado,
                                    @Param("responsableId") String responsableId,
                                    @Param("cierreDesde") java.time.LocalDate cierreDesde,
                                    @Param("cierreHasta") java.time.LocalDate cierreHasta,
                                    @Param("soloPagosPendientes") boolean soloPagosPendientes,
                                    Pageable pageable);
}
