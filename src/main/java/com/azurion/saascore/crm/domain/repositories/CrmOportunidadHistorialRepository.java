package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmOportunidadHistorial;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrmOportunidadHistorialRepository extends JpaRepository<CrmOportunidadHistorial, Long> {

    @EntityGraph(attributePaths = {"etapaOrigen", "etapaDestino"})
    List<CrmOportunidadHistorial> findByOportunidadIdOrderByFechaCambioDescIdDesc(Long oportunidadId);
}
