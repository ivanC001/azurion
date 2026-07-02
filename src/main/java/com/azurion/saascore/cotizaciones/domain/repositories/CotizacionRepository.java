package com.azurion.saascore.cotizaciones.domain.repositories;

import com.azurion.saascore.cotizaciones.domain.entities.Cotizacion;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CotizacionRepository extends JpaRepository<Cotizacion, Long> {

    @EntityGraph(attributePaths = {"cliente", "sucursal", "detalles", "detalles.producto", "detalles.promocion"})
    List<Cotizacion> findAllByOrderByFechaEmisionDescIdDesc();

    @EntityGraph(attributePaths = {"cliente", "sucursal", "detalles", "detalles.producto", "detalles.promocion"})
    List<Cotizacion> findByCrmOportunidadIdOrderByFechaEmisionDescIdDesc(Long crmOportunidadId);
}
