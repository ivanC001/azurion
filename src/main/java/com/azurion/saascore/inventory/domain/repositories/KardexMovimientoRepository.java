package com.azurion.saascore.inventory.domain.repositories;

import com.azurion.saascore.inventory.domain.entities.KardexMovimiento;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KardexMovimientoRepository extends JpaRepository<KardexMovimiento, Long> {
    @EntityGraph(attributePaths = {"producto", "almacen", "lote"})
    List<KardexMovimiento> findByLoteIdOrderByFechaMovimientoDesc(Long loteId);

    @EntityGraph(attributePaths = {"producto", "almacen"})
    List<KardexMovimiento> findByProductoIdAndAlmacenIdOrderByFechaMovimientoDesc(Long productoId, Long almacenId);
    @EntityGraph(attributePaths = {"producto", "almacen"})
    List<KardexMovimiento> findByProductoIdOrderByFechaMovimientoDesc(Long productoId);
    @EntityGraph(attributePaths = {"producto", "almacen"})
    List<KardexMovimiento> findByAlmacenIdOrderByFechaMovimientoDesc(Long almacenId);
    @EntityGraph(attributePaths = {"producto", "almacen"})
    List<KardexMovimiento> findAllByOrderByFechaMovimientoDesc();
}
