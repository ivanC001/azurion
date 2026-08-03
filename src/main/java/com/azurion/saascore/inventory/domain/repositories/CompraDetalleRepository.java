package com.azurion.saascore.inventory.domain.repositories;

import com.azurion.saascore.inventory.domain.entities.CompraDetalle;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;

public interface CompraDetalleRepository extends JpaRepository<CompraDetalle, Long> {

    @EntityGraph(attributePaths = {"compra", "producto"})
    List<CompraDetalle> findByCompraId(Long compraId);

    @EntityGraph(attributePaths = {"compra", "producto"})
    List<CompraDetalle> findByCompraIdIn(List<Long> compraIds);

    @Query("""
            select coalesce(sum(
                detalle.cantidad * (
                    coalesce(detalle.precioVentaNeto, detalle.precioVenta, 0)
                    - coalesce(detalle.costoInventariableUnitario, detalle.costoUnitario)
                )
            ), 0)
              from CompraDetalle detalle
             where detalle.compra.estado = 'REGISTRADA'
               and detalle.compra.tratamientoIgv = 'DESGLOSADO'
            """)
    BigDecimal sumProjectedProfit();

    @Query("""
            select coalesce(sum(detalle.totalCostoInventariable), 0)
              from CompraDetalle detalle
             where detalle.compra.estado = 'REGISTRADA'
            """)
    BigDecimal sumRegisteredInventoryCost();
}
