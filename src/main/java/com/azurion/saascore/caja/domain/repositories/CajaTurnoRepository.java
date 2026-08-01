package com.azurion.saascore.caja.domain.repositories;

import com.azurion.saascore.caja.domain.entities.CajaTurno;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CajaTurnoRepository extends JpaRepository<CajaTurno, Long> {

    @Override
    @EntityGraph(attributePaths = "caja.sucursal")
    Optional<CajaTurno> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "caja.sucursal")
    @Query("select turno from CajaTurno turno where turno.id = :id")
    Optional<CajaTurno> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = "caja.sucursal")
    Optional<CajaTurno> findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(
            Long usuarioId,
            String estado
    );

    @EntityGraph(attributePaths = "caja.sucursal")
    Optional<CajaTurno> findFirstByCajaIdAndEstadoOrderByFechaAperturaDesc(
            Long cajaId,
            String estado
    );

    @EntityGraph(attributePaths = "caja.sucursal")
    List<CajaTurno> findAllByOrderByFechaAperturaDesc();

    @EntityGraph(attributePaths = "caja.sucursal")
    List<CajaTurno> findAllByOrderByFechaAperturaDesc(Pageable pageable);

    @EntityGraph(attributePaths = "caja.sucursal")
    List<CajaTurno> findByEstadoOrderByFechaAperturaDesc(String estado);

    @EntityGraph(attributePaths = "caja.sucursal")
    List<CajaTurno> findByEstadoOrderByFechaAperturaDesc(String estado, Pageable pageable);

    @EntityGraph(attributePaths = "caja.sucursal")
    List<CajaTurno> findByCajaSucursalIdOrderByFechaAperturaDesc(Long sucursalId);

    @EntityGraph(attributePaths = "caja.sucursal")
    List<CajaTurno> findByCajaSucursalIdOrderByFechaAperturaDesc(Long sucursalId, Pageable pageable);

    @EntityGraph(attributePaths = "caja.sucursal")
    List<CajaTurno> findByCajaSucursalIdAndEstadoOrderByFechaAperturaDesc(
            Long sucursalId,
            String estado
    );

    @EntityGraph(attributePaths = "caja.sucursal")
    List<CajaTurno> findByCajaSucursalIdAndEstadoOrderByFechaAperturaDesc(
            Long sucursalId,
            String estado,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "caja.sucursal")
    List<CajaTurno> findByUsuarioIdOrderByFechaAperturaDesc(Long usuarioId);

    @EntityGraph(attributePaths = "caja.sucursal")
    List<CajaTurno> findByUsuarioIdOrderByFechaAperturaDesc(Long usuarioId, Pageable pageable);

    @EntityGraph(attributePaths = "caja.sucursal")
    List<CajaTurno> findByUsuarioIdAndEstadoOrderByFechaAperturaDesc(Long usuarioId, String estado);

    @EntityGraph(attributePaths = "caja.sucursal")
    List<CajaTurno> findByUsuarioIdAndEstadoOrderByFechaAperturaDesc(
            Long usuarioId,
            String estado,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "caja.sucursal")
    List<CajaTurno> findByUsuarioIdAndCajaSucursalIdOrderByFechaAperturaDesc(
            Long usuarioId,
            Long sucursalId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "caja.sucursal")
    List<CajaTurno> findByUsuarioIdAndCajaSucursalIdAndEstadoOrderByFechaAperturaDesc(
            Long usuarioId,
            Long sucursalId,
            String estado,
            Pageable pageable
    );
}
