package com.azurion.saascore.sucursales.domain.repositories;

import com.azurion.saascore.sucursales.domain.entities.Sucursal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SucursalRepository extends JpaRepository<Sucursal, Long> {

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

    long countByActivoTrue();

    List<Sucursal> findAllByOrderByNombreAsc();
}
