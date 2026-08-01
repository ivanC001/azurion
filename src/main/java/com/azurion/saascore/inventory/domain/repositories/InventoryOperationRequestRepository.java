package com.azurion.saascore.inventory.domain.repositories;

import com.azurion.saascore.inventory.domain.entities.InventoryOperationRequest;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryOperationRequestRepository extends JpaRepository<InventoryOperationRequest, Long> {

    @EntityGraph(attributePaths = {
            "kardexMovimiento",
            "kardexMovimiento.producto",
            "kardexMovimiento.almacen"
    })
    Optional<InventoryOperationRequest> findByOperationKey(String operationKey);
}
