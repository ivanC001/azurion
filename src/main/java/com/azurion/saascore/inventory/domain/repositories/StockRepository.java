package com.azurion.saascore.inventory.domain.repositories;

import com.azurion.saascore.inventory.domain.entities.Stock;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, Long> {
    @EntityGraph(attributePaths = {"producto", "almacen"})
    Optional<Stock> findByProductoIdAndAlmacenId(Long productoId, Long almacenId);
    @EntityGraph(attributePaths = {"producto", "almacen"})
    List<Stock> findByAlmacenId(Long almacenId);
    @EntityGraph(attributePaths = {"producto", "almacen"})
    List<Stock> findByProductoId(Long productoId);
    @EntityGraph(attributePaths = {"producto", "almacen", "almacen.sucursal"})
    List<Stock> findByAlmacenSucursalId(Long sucursalId);
    @Override
    @EntityGraph(attributePaths = {"producto", "almacen"})
    List<Stock> findAll();
}
