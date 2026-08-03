package com.azurion.saascore.ventas.domain.repositories;

import com.azurion.saascore.ventas.domain.entities.VentaDetalle;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VentaDetalleRepository extends JpaRepository<VentaDetalle, Long> {
    List<VentaDetalle> findByVentaIdOrderByIdAsc(Long ventaId);

    @Query("""
            select coalesce(sum(detalle.baseImponible), 0)
              from VentaDetalle detalle
             where detalle.venta.fechaVenta >= :from
               and detalle.venta.fechaVenta < :to
            """)
    BigDecimal sumNetSalesBetween(@Param("from") OffsetDateTime from,
                                  @Param("to") OffsetDateTime to);

    @Query("""
            select coalesce(sum(detalle.montoIgv), 0)
              from VentaDetalle detalle
             where detalle.venta.fechaVenta >= :from
               and detalle.venta.fechaVenta < :to
            """)
    BigDecimal sumSalesTaxBetween(@Param("from") OffsetDateTime from,
                                  @Param("to") OffsetDateTime to);

    @Query("""
            select coalesce(sum(detalle.cantidad * detalle.costoUnitarioInventariable), 0)
              from VentaDetalle detalle
             where detalle.venta.fechaVenta >= :from
               and detalle.venta.fechaVenta < :to
               and detalle.costoUnitarioInventariable is not null
            """)
    BigDecimal sumKnownInventoryCostBetween(@Param("from") OffsetDateTime from,
                                             @Param("to") OffsetDateTime to);

    @Query("""
            select count(detalle)
              from VentaDetalle detalle
             where detalle.venta.fechaVenta >= :from
               and detalle.venta.fechaVenta < :to
               and detalle.costoUnitarioInventariable is null
            """)
    long countMissingInventoryCostBetween(@Param("from") OffsetDateTime from,
                                          @Param("to") OffsetDateTime to);
}
