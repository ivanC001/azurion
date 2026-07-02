package com.azurion.saascore.inventory.domain.repositories;

import com.azurion.saascore.inventory.domain.entities.Compra;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    @EntityGraph(attributePaths = {"almacen", "detalles", "detalles.producto"})
    Optional<Compra> findById(Long id);

    @EntityGraph(attributePaths = {"almacen", "detalles", "detalles.producto"})
    Optional<Compra> findFirstByNumeroComprobanteIgnoreCase(String numeroComprobante);

    List<Compra> findByNumeroComprobanteIgnoreCase(String numeroComprobante);

    @EntityGraph(attributePaths = {"almacen"})
    List<Compra> findAllByOrderByFechaIngresoDesc();
}
