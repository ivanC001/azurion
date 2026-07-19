package com.azurion.saascore.caja.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "venta_facturacion_outbox", schema = "public")
public class VentaFacturacionOutbox extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, length = 120)
    private String tenantId;

    @Column(name = "tenant_ruc", nullable = false, length = 20)
    private String tenantRuc;

    @Column(name = "venta_id", nullable = false)
    private Long ventaId;

    @Column(name = "external_id", nullable = false, length = 180)
    private String externalId;

    @Column(nullable = false, length = 180)
    private String endpoint;

    @Column(name = "tipo_comprobante", nullable = false, length = 40)
    private String tipoComprobante;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(nullable = false, length = 30)
    private String status = "PENDING";

    @Column(nullable = false)
    private Integer attempts = 0;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "lease_owner", length = 120)
    private String leaseOwner;

    @Column(name = "lease_until")
    private LocalDateTime leaseUntil;

    @Column(name = "heartbeat_at")
    private LocalDateTime heartbeatAt;
}
