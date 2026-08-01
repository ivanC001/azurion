package com.azurion.saascore.inventory.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "inventory_operation_requests",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_inventory_operation_request_key",
                columnNames = "operation_key"
        )
)
public class InventoryOperationRequest extends BaseEntity {

    @Column(name = "operation_key", nullable = false, length = 100)
    private String operationKey;

    @Column(name = "operation_type", nullable = false, length = 30)
    private String operationType;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kardex_movimiento_id", nullable = false)
    private KardexMovimiento kardexMovimiento;
}
