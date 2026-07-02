package com.azurion.saascore.planes.domain.repositories;

import com.azurion.saascore.planes.domain.entities.Plan;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    Optional<Plan> findByCodigoIgnoreCase(String codigo);
}
