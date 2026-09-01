package com.azurion.saascore.crm.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Tarea de reenganche: una plantilla que se enviara a un prospecto en una fecha
 * futura, cuando la ventana de atencion de 24 horas ya este cerrada.
 *
 * <p>Vive en el esquema {@code public} para que el worker pueda sondearla sin
 * contexto de tenant; {@code prospectoId} se resuelve recien despues de fijar el
 * tenant que indica {@code tenantId}.
 */
@Getter
@Setter
@Entity
@Table(name = "crm_whatsapp_reengagement_outbox", schema = "public")
public class CrmWhatsappReengagementOutbox extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, length = 120)
    private String tenantId;

    @Column(name = "prospecto_id", nullable = false)
    private Long prospectoId;

    @Column(name = "dedupe_key", nullable = false, length = 180)
    private String dedupeKey;

    @Column(name = "plantilla_nombre", nullable = false, length = 512)
    private String plantillaNombre;

    @Column(name = "plantilla_idioma", nullable = false, length = 35)
    private String plantillaIdioma;

    @Column(name = "parametros_json", columnDefinition = "TEXT")
    private String parametrosJson;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

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

    @Column(name = "creado_por", length = 120)
    private String creadoPor;

    @Column(length = 500)
    private String resultado;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
