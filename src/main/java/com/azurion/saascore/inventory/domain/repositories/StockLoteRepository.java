package com.azurion.saascore.inventory.domain.repositories;

import com.azurion.saascore.inventory.domain.entities.StockLote;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockLoteRepository extends JpaRepository<StockLote, Long> {
    @EntityGraph(attributePaths = {"lote", "producto", "almacen"})
    Optional<StockLote> findByLoteIdAndAlmacenId(Long loteId, Long almacenId);

    @EntityGraph(attributePaths = {"lote", "producto", "almacen"})
    List<StockLote> findByProductoIdAndAlmacenIdOrderByLoteFechaVencimientoAscLoteFechaIngresoAsc(Long productoId, Long almacenId);

    @EntityGraph(attributePaths = {"lote", "producto", "almacen"})
    List<StockLote> findByProductoIdOrderByLoteFechaVencimientoAscLoteFechaIngresoAsc(Long productoId);
}
