package com.azurion.saascore.caja.domain.repositories;

import com.azurion.saascore.caja.domain.entities.CajaFisica;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CajaFisicaRepository extends JpaRepository<CajaFisica, Long> {

    @Override
    @EntityGraph(attributePaths = "sucursal")
    Optional<CajaFisica> findById(Long id);

    @EntityGraph(attributePaths = "sucursal")
    Optional<CajaFisica> findBySucursalIdAndCodigoIgnoreCase(Long sucursalId, String codigo);

    @EntityGraph(attributePaths = "sucursal")
    List<CajaFisica> findAllByOrderBySucursalNombreAscNombreAsc();

    @Query(value = """
            SELECT caja.*
            FROM cajas caja
            JOIN usuario_cajas asignacion ON asignacion.caja_id = caja.id
            WHERE asignacion.usuario_id = :usuarioId
            ORDER BY caja.nombre
            """, nativeQuery = true)
    List<CajaFisica> findPermitidas(@Param("usuarioId") Long usuarioId);
}
