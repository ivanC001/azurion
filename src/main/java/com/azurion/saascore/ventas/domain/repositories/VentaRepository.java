package com.azurion.saascore.ventas.domain.repositories;

import com.azurion.saascore.ventas.domain.entities.Venta;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VentaRepository extends JpaRepository<Venta, Long> {
    Optional<Venta> findByExternalId(String externalId);

    List<Venta> findAllByOrderByFechaVentaDesc();
}
