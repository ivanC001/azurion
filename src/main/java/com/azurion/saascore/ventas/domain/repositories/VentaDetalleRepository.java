package com.azurion.saascore.ventas.domain.repositories;

import com.azurion.saascore.ventas.domain.entities.VentaDetalle;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VentaDetalleRepository extends JpaRepository<VentaDetalle, Long> {
    List<VentaDetalle> findByVentaIdOrderByIdAsc(Long ventaId);
}
