package com.azurion.saascore.inventory.domain.repositories;

import com.azurion.saascore.inventory.domain.entities.Stock;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    @EntityGraph(attributePaths = {"producto", "almacen"})
    @Query("""
            select stock from Stock stock
             where (:productoId is null or stock.producto.id = :productoId)
               and (:almacenId is null or stock.almacen.id = :almacenId)
            """)
    Page<Stock> search(@Param("productoId") Long productoId,
                       @Param("almacenId") Long almacenId,
                       Pageable pageable);

    @Query("""
            select stock.producto.id, sum(stock.cantidad)
              from Stock stock
             where stock.producto.id in :productoIds
               and (:almacenId is null or stock.almacen.id = :almacenId)
             group by stock.producto.id
            """)
    List<Object[]> sumCantidadByProductoIds(@Param("productoIds") List<Long> productoIds,
                                            @Param("almacenId") Long almacenId);

    @Query("""
            select coalesce(sum(stock.cantidad), 0)
              from Stock stock
             where stock.producto.id = :productoId
            """)
    java.math.BigDecimal sumCantidadByProductoId(@Param("productoId") Long productoId);

    @Query("""
            select coalesce(sum(stock.cantidad), 0)
              from Stock stock
             where stock.almacen.id = :almacenId
            """)
    java.math.BigDecimal sumCantidadByAlmacenId(@Param("almacenId") Long almacenId);

    @Query("""
            select count(stock) from Stock stock
             where stock.producto.manejaStock = true
               and stock.cantidad <= 0
            """)
    long countWithoutStock();

    @Query("""
            select count(stock) from Stock stock
             where stock.producto.manejaStock = true
               and stock.cantidad > 0
               and stock.cantidad <=
                   case
                       when stock.stockMinimo > 0 then stock.stockMinimo
                       when stock.producto.stockMinimoGlobal > 0 then stock.producto.stockMinimoGlobal
                       else coalesce(stock.producto.stockMinimo, 0)
                   end
            """)
    long countLowStock();
}
