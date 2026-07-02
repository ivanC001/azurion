package com.azurion.saascore.inventory.domain.repositories;

import com.azurion.saascore.inventory.domain.entities.Producto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findBySku(String sku);
    Optional<Producto> findBySkuIgnoreCase(String sku);
    boolean existsByCodigoIgnoreCase(String codigo);
    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

    @EntityGraph(attributePaths = "almacen")
    List<Producto> findByAlmacenIdOrderByNombreAsc(Long almacenId);

    @EntityGraph(attributePaths = "almacen")
    List<Producto> findAllByOrderByNombreAsc();
}
