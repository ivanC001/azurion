package com.azurion.saascore.inventory.domain.repositories;

import com.azurion.saascore.inventory.domain.entities.CompraDetalle;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompraDetalleRepository extends JpaRepository<CompraDetalle, Long> {

    @EntityGraph(attributePaths = {"compra", "producto"})
    List<CompraDetalle> findByCompraId(Long compraId);
}
