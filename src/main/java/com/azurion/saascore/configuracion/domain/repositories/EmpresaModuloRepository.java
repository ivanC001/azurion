package com.azurion.saascore.configuracion.domain.repositories;

import com.azurion.saascore.configuracion.domain.entities.EmpresaModulo;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EmpresaModuloRepository extends JpaRepository<EmpresaModulo, Long> {

    long countByEmpresaId(Long empresaId);

    @EntityGraph(attributePaths = {"modulo"})
    List<EmpresaModulo> findByEmpresaIdOrderByModuloNombreAsc(Long empresaId);

    Optional<EmpresaModulo> findByEmpresaIdAndModuloId(Long empresaId, Long moduloId);

    @Query("""
            select em from EmpresaModulo em
            join fetch em.modulo m
            where em.empresa.id = :empresaId
            order by m.nombre
            """)
    List<EmpresaModulo> findDetailedByEmpresaId(Long empresaId);

    @Query("""
            select m.codigo
            from EmpresaModulo em
            join em.modulo m
            where em.empresa.id = :empresaId
              and em.activo = true
              and upper(coalesce(em.estado, 'ACTIVO')) = 'ACTIVO'
              and upper(coalesce(m.estado, 'ACTIVO')) = 'ACTIVO'
              and (em.fechaInicio is null or em.fechaInicio <= :today)
              and (em.fechaFin is null or em.fechaFin >= :today)
            order by m.codigo
            """)
    List<String> findActiveModuleCodes(Long empresaId, LocalDate today);

    @Query("""
            select count(em) > 0
            from EmpresaModulo em
            join em.modulo m
            where em.empresa.id = :empresaId
              and upper(m.codigo) = upper(:moduloCodigo)
              and em.activo = true
              and upper(coalesce(em.estado, 'ACTIVO')) = 'ACTIVO'
              and upper(coalesce(m.estado, 'ACTIVO')) = 'ACTIVO'
              and (em.fechaInicio is null or em.fechaInicio <= :today)
              and (em.fechaFin is null or em.fechaFin >= :today)
            """)
    boolean existsActiveModule(Long empresaId, String moduloCodigo, LocalDate today);
}
