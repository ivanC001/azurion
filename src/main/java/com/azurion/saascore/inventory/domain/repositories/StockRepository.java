package com.azurion.saascore.inventory.domain.repositories;

import com.azurion.saascore.inventory.domain.entities.Stock;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            select stock.producto.id, sum(stock.cantidad)
              from Stock stock
             where stock.producto.id in :productoIds
               and (:almacenId is null or stock.almacen.id = :almacenId)
             group by stock.producto.id
            """)
    List<Object[]> sumCantidadByProductoIds(@Param("productoIds") List<Long> productoIds,
                                            @Param("almacenId") Long almacenId);
}
