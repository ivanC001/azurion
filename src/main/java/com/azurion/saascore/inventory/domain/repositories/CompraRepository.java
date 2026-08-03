package com.azurion.saascore.inventory.domain.repositories;

import com.azurion.saascore.inventory.domain.entities.Compra;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    @EntityGraph(attributePaths = {"almacen", "detalles", "detalles.producto"})
    Optional<Compra> findById(Long id);

    @EntityGraph(attributePaths = {"almacen", "detalles", "detalles.producto"})
    Optional<Compra> findFirstByNumeroComprobanteIgnoreCase(String numeroComprobante);

    @EntityGraph(attributePaths = {"almacen", "detalles", "detalles.producto"})
    Optional<Compra> findByClientOperationId(String clientOperationId);

    List<Compra> findByNumeroComprobanteIgnoreCase(String numeroComprobante);

    @EntityGraph(attributePaths = {"almacen"})
    List<Compra> findAllByOrderByFechaIngresoDesc();

    @EntityGraph(attributePaths = {"almacen"})
    @Query("""
            select compra from Compra compra
             where (:almacenId is null or compra.almacen.id = :almacenId)
               and (:query = ''
                    or lower(compra.numeroComprobante) like lower(concat('%', :query, '%'))
                    or lower(coalesce(compra.proveedorDocumento, '')) like lower(concat('%', :query, '%'))
                    or lower(coalesce(compra.proveedorNombre, '')) like lower(concat('%', :query, '%')))
            """)
    Page<Compra> search(@Param("query") String query,
                        @Param("almacenId") Long almacenId,
                        Pageable pageable);

    @Query("select coalesce(sum(compra.total), 0) from Compra compra where compra.estado = 'REGISTRADA'")
    BigDecimal sumRegisteredTotal();

    @Query("""
            select coalesce(sum(compra.subtotalNeto), 0)
              from Compra compra
             where compra.estado = 'REGISTRADA'
               and compra.fechaEmision >= :from
               and compra.fechaEmision <= :to
            """)
    BigDecimal sumNetBetween(@Param("from") java.time.LocalDate from,
                             @Param("to") java.time.LocalDate to);

    @Query("""
            select coalesce(sum(compra.montoIgv), 0)
              from Compra compra
             where compra.estado = 'REGISTRADA'
               and compra.fechaEmision >= :from
               and compra.fechaEmision <= :to
            """)
    BigDecimal sumPurchaseTaxBetween(@Param("from") java.time.LocalDate from,
                                     @Param("to") java.time.LocalDate to);

    @Query("""
            select coalesce(sum(case when compra.creditoFiscalAplicable = true then compra.montoIgv else 0 end), 0)
              from Compra compra
             where compra.estado = 'REGISTRADA'
               and compra.fechaEmision >= :from
               and compra.fechaEmision <= :to
            """)
    BigDecimal sumTaxCreditBetween(@Param("from") java.time.LocalDate from,
                                   @Param("to") java.time.LocalDate to);
}
