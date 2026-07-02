package com.azurion.saascore.suscripciones.domain.repositories;

import com.azurion.saascore.suscripciones.domain.entities.Suscripcion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SuscripcionRepository extends JpaRepository<Suscripcion, Long> {

    List<Suscripcion> findByEmpresaId(Long empresaId);
}
