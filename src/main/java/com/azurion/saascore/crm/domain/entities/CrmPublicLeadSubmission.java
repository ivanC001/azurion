package com.azurion.saascore.crm.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "crm_public_lead_submissions")
public class CrmPublicLeadSubmission extends BaseEntity {

    @Column(name = "receipt_id", nullable = false, unique = true, length = 64)
    private String receiptId;

    @Column(name = "idempotency_hash", unique = true, length = 64)
    private String idempotencyHash;

    @Column(name = "source_key", length = 120)
    private String sourceKey;

    @Column(name = "source_type", nullable = false, length = 20)
    private String sourceType;

    @Column(name = "prospecto_id")
    private Long prospectoId;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "RECEIVED";

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt = OffsetDateTime.now();

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;
}
