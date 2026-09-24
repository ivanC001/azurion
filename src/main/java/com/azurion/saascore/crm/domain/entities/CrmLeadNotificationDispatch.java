package com.azurion.saascore.crm.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Un aviso encolado. Se guarda antes de enviarlo para que el correo salga fuera de
 * la transaccion que registro el lead: si el SMTP falla, el lead ya quedo grabado.
 */
@Getter
@Setter
@Entity
@Table(name = "crm_lead_notification_dispatch")
public class CrmLeadNotificationDispatch extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prospecto_id", nullable = false)
    private CrmProspecto prospecto;

    /** LEAD_NUEVO o MENSAJE_NUEVO. */
    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    @Column(name = "destinatarios", nullable = false, length = 1000)
    private String destinatarios;

    @Column(name = "asunto", nullable = false, length = 300)
    private String asunto;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "PENDIENTE";

    @Column(name = "detalle", length = 500)
    private String detalle;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
