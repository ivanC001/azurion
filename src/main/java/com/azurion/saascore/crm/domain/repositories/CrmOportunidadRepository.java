package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmOportunidad;
import java.math.BigDecimal;
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

    interface AggregateProjection {
        String getCodigo();
        long getCantidad();
        BigDecimal getMonto();
    }

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

    @Query("select count(o.id) from CrmOportunidad o where (:responsableId is null or o.responsableId = :responsableId)")
    long countScoped(@Param("responsableId") String responsableId);

    @Query("""
            select count(o.id)
            from CrmOportunidad o
            where (:responsableId is null or o.responsableId = :responsableId)
              and (o.etapa = 'COTIZADO' or o.estado = 'GANADA')
            """)
    long countQuotedScoped(@Param("responsableId") String responsableId);

    @Query("""
            select coalesce(sum(o.montoEstimado), 0)
            from CrmOportunidad o
            where (:responsableId is null or o.responsableId = :responsableId)
              and o.estado = 'ABIERTA'
            """)
    BigDecimal sumOpenPipelineScoped(@Param("responsableId") String responsableId);

    @Query("""
            select coalesce(sum(coalesce(o.montoReal, o.montoEstimado)), 0)
            from CrmOportunidad o
            where (:responsableId is null or o.responsableId = :responsableId)
              and o.estado = :estado
            """)
    BigDecimal sumRealByEstadoScoped(@Param("responsableId") String responsableId,
                                     @Param("estado") String estado);

    @Query("""
            select e.codigo as codigo,
                   count(o.id) as cantidad,
                   coalesce(sum(o.montoEstimado), 0) as monto
            from CrmOportunidad o
            join o.etapaPipeline e
            where (:responsableId is null or o.responsableId = :responsableId)
            group by e.codigo
            """)
    List<AggregateProjection> summarizeByStageScoped(@Param("responsableId") String responsableId);

    @Query("""
            select coalesce(o.responsableId, 'SIN_ASIGNAR') as codigo,
                   count(o.id) as cantidad,
                   coalesce(sum(o.montoEstimado), 0) as monto
            from CrmOportunidad o
            where (:responsableId is null or o.responsableId = :responsableId)
            group by coalesce(o.responsableId, 'SIN_ASIGNAR')
            order by count(o.id) desc
            """)
    List<AggregateProjection> summarizeByOwnerScoped(@Param("responsableId") String responsableId);

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
