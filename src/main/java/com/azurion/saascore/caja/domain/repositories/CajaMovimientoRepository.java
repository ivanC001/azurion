package com.azurion.saascore.caja.domain.repositories;

import com.azurion.saascore.caja.domain.entities.CajaMovimiento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CajaMovimientoRepository extends JpaRepository<CajaMovimiento, Long> {

    List<CajaMovimiento> findByCajaIdOrderByFechaMovimientoDesc(Long cajaId);
}
