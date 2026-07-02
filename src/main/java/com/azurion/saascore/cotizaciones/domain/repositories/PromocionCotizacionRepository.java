package com.azurion.saascore.cotizaciones.domain.repositories;

import com.azurion.saascore.cotizaciones.domain.entities.PromocionCotizacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromocionCotizacionRepository extends JpaRepository<PromocionCotizacion, Long> {

    List<PromocionCotizacion> findAllByOrderByIdDesc();

    boolean existsByCodigoIgnoreCase(String codigo);
}
