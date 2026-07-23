package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmProspecto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CrmProspectoRepository extends JpaRepository<CrmProspecto, Long>, JpaSpecificationExecutor<CrmProspecto> {

    List<CrmProspecto> findAllByOrderByIdDesc();

    List<CrmProspecto> findByResponsableIdOrderByIdDesc(String responsableId);

    List<CrmProspecto> findByResponsableIdInOrderByIdDesc(List<String> responsableIds);

    long countByEstado(String estado);

    long countByResponsableIdAndEstado(String responsableId, String estado);

    long countByResponsableId(String responsableId);

    long countByCanalIngreso(String canalIngreso);

    long countByCanalIngresoNot(String canalIngreso);

    Optional<CrmProspecto> findFirstByTelefonoOrderByIdDesc(String telefono);

    @Query(value = """
            select *
            from crm_prospectos
            where regexp_replace(coalesce(telefono, ''), '[^0-9]', '', 'g') = :telefono
            order by id desc
            limit 1
            """, nativeQuery = true)
    Optional<CrmProspecto> findFirstByTelefonoNormalizado(@Param("telefono") String telefono);

    Optional<CrmProspecto> findFirstByCorreoIgnoreCaseOrderByIdDesc(String correo);

    @Query("""
            select p
            from CrmProspecto p
            where (:scopeAll = true or p.responsableId in :responsableScope)
              and (:query is null
                   or lower(coalesce(p.nombre, '')) like concat('%', :query, '%')
                   or lower(coalesce(p.razonSocial, '')) like concat('%', :query, '%')
                   or lower(coalesce(p.nombreComercial, '')) like concat('%', :query, '%')
                   or lower(coalesce(p.telefono, '')) like concat('%', :query, '%')
                   or lower(coalesce(p.correo, '')) like concat('%', :query, '%')
                   or lower(coalesce(p.numeroDocumento, '')) like concat('%', :query, '%')
                   or lower(coalesce(p.interesPrincipal, '')) like concat('%', :query, '%')
                   or lower(coalesce(p.campania, '')) like concat('%', :query, '%'))
              and (:estado is null or p.estado = :estado)
              and (:origen is null or p.origen = :origen)
              and (:canalIngreso is null or p.canalIngreso = :canalIngreso)
              and (:campania is null or lower(coalesce(p.campania, '')) = :campania)
              and (:responsableId is null or p.responsableId = :responsableId)
              and (:fechaDesde is null or p.fechaInteres >= :fechaDesde)
              and (:fechaHasta is null or p.fechaInteres <= :fechaHasta)
            """)
    Page<CrmProspecto> searchPage(@Param("scopeAll") boolean scopeAll,
                                  @Param("responsableScope") List<String> responsableScope,
                                  @Param("query") String query,
                                  @Param("estado") String estado,
                                  @Param("origen") String origen,
                                  @Param("canalIngreso") String canalIngreso,
                                  @Param("campania") String campania,
                                  @Param("responsableId") String responsableId,
                                  @Param("fechaDesde") java.time.LocalDate fechaDesde,
                                  @Param("fechaHasta") java.time.LocalDate fechaHasta,
                                  Pageable pageable);
}
