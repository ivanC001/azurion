package com.azurion.saascore.inventory.domain.repositories;

import com.azurion.saascore.inventory.domain.entities.KardexMovimiento;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    @EntityGraph(attributePaths = {"producto", "almacen"})
    @Query("""
            select movimiento from KardexMovimiento movimiento
             where (:productoId is null or movimiento.producto.id = :productoId)
               and (:almacenId is null or movimiento.almacen.id = :almacenId)
            """)
    Page<KardexMovimiento> search(@Param("productoId") Long productoId,
                                  @Param("almacenId") Long almacenId,
                                  Pageable pageable);
}
