package com.azurion.shared.audit;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "auditoria_global", schema = "public")
public class AuditoriaGlobal extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, length = 80)
    private String tenantId;

    @Column(name = "user_id", length = 80)
    private String userId;

    @Column(name = "method", nullable = false, length = 10)
    private String method;

    @Column(name = "path", nullable = false, length = 255)
    private String path;

    @Column(name = "status_code", nullable = false)
    private int statusCode;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Column(name = "message", length = 1000)
    private String message;
}
