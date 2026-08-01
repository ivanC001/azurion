package com.azurion.saascore.inventory.domain.repositories;

import com.azurion.saascore.inventory.domain.entities.StockLote;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StockLoteRepository extends JpaRepository<StockLote, Long> {
    @EntityGraph(attributePaths = {"lote", "producto", "almacen"})
    Optional<StockLote> findByLoteIdAndAlmacenId(Long loteId, Long almacenId);

    @EntityGraph(attributePaths = {"lote", "producto", "almacen"})
    List<StockLote> findByProductoIdAndAlmacenIdOrderByLoteFechaVencimientoAscLoteFechaIngresoAsc(Long productoId, Long almacenId);

    @EntityGraph(attributePaths = {"lote", "producto", "almacen"})
    List<StockLote> findByProductoIdOrderByLoteFechaVencimientoAscLoteFechaIngresoAsc(Long productoId);

    @EntityGraph(attributePaths = {"lote", "producto", "almacen"})
    List<StockLote> findByAlmacenIdOrderByLoteFechaVencimientoAscLoteFechaIngresoAsc(Long almacenId);

    @EntityGraph(attributePaths = {"lote", "producto", "almacen"})
    @Query("""
            select stockLote from StockLote stockLote
             where (:productoId is null or stockLote.producto.id = :productoId)
               and (:almacenId is null or stockLote.almacen.id = :almacenId)
               and stockLote.estado = 'ACTIVO'
               and stockLote.stockActual > 0
            """)
    Page<StockLote> search(@Param("productoId") Long productoId,
                           @Param("almacenId") Long almacenId,
                           Pageable pageable);

    @Query("""
            select count(stockLote) from StockLote stockLote
             where stockLote.estado = 'ACTIVO'
               and stockLote.stockActual > 0
               and stockLote.lote.fechaVencimiento < :today
            """)
    long countExpired(@Param("today") LocalDate today);

    @Query("""
            select count(stockLote) from StockLote stockLote
             where stockLote.estado = 'ACTIVO'
               and stockLote.stockActual > 0
               and stockLote.lote.fechaVencimiento between :today and :limit
            """)
    long countExpiring(@Param("today") LocalDate today, @Param("limit") LocalDate limit);
}
