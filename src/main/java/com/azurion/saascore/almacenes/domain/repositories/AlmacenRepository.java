package com.azurion.saascore.almacenes.domain.repositories;

import com.azurion.saascore.almacenes.domain.entities.Almacen;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AlmacenRepository extends JpaRepository<Almacen, Long> {
    Optional<Almacen> findByCodigo(String codigo);

    @Override
    @EntityGraph(attributePaths = "sucursal")
    List<Almacen> findAll();

    @Override
    @EntityGraph(attributePaths = "sucursal")
    Page<Almacen> findAll(Pageable pageable);
}
