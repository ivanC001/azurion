package com.azurion.saascore.suscripciones.domain.repositories;

import com.azurion.saascore.suscripciones.domain.entities.Suscripcion;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface SuscripcionRepository extends JpaRepository<Suscripcion, Long> {

    @EntityGraph(attributePaths = {"empresa", "plan"})
    List<Suscripcion> findByEmpresaIdOrderByIdDesc(Long empresaId);

    @EntityGraph(attributePaths = {"empresa", "plan"})
    List<Suscripcion> findAllByOrderByIdDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"empresa", "plan"})
    @Query("""
            select s
            from Suscripcion s
            where s.empresa.id = :empresaId
              and upper(s.estado) = 'ACTIVA'
            order by s.id desc
            """)
    List<Suscripcion> findAllActiveStateForUpdate(@Param("empresaId") Long empresaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "plan")
    @Query("""
            select s
            from Suscripcion s
            where s.empresa.id = :empresaId
              and upper(s.estado) = 'ACTIVA'
              and s.fechaInicio <= current_date
              and (s.fechaFin is null or s.fechaFin >= current_date)
            order by s.fechaInicio desc, s.id desc
            """)
    List<Suscripcion> findActiveForUpdate(@Param("empresaId") Long empresaId);

    @EntityGraph(attributePaths = "plan")
    @Query("""
            select s
            from Suscripcion s
            where s.empresa.id = :empresaId
              and upper(s.estado) = 'ACTIVA'
              and s.fechaInicio <= current_date
              and (s.fechaFin is null or s.fechaFin >= current_date)
            order by s.fechaInicio desc, s.id desc
            """)
    List<Suscripcion> findActive(@Param("empresaId") Long empresaId);
}
