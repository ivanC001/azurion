package com.azurion.saascore.inventory.domain.repositories;

import com.azurion.saascore.inventory.domain.entities.Lote;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoteRepository extends JpaRepository<Lote, Long> {
    @EntityGraph(attributePaths = {"producto", "compraDetalle", "compraDetalle.compra"})
    Optional<Lote> findById(Long id);

    @EntityGraph(attributePaths = {"producto"})
    Optional<Lote> findByProductoIdAndCodigoLote(Long productoId, String codigoLote);

    @EntityGraph(attributePaths = {"producto"})
    List<Lote> findByProductoIdOrderByFechaVencimientoAscFechaIngresoAsc(Long productoId);

    @EntityGraph(attributePaths = {"producto"})
    List<Lote> findByProductoIdAndEstadoAndFechaVencimientoGreaterThanEqualOrderByFechaVencimientoAscFechaIngresoAsc(
            Long productoId,
            String estado,
            LocalDate fechaVencimiento
    );
}
