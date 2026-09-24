package com.azurion.saascore.crm.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Preferencias del tenant para avisar por correo cuando entra trabajo nuevo al CRM.
 *
 * El envio usa el SMTP que el tenant ya configuro para sus cotizaciones, asi que
 * aqui solo se decide a quien avisar y con que frecuencia.
 */
@Getter
@Setter
@Entity
@Table(name = "crm_lead_notification_config")
public class CrmLeadNotificationConfig extends BaseEntity {

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "notificar_lead_nuevo", nullable = false)
    private boolean notificarLeadNuevo = true;

    @Column(name = "notificar_mensaje_nuevo", nullable = false)
    private boolean notificarMensajeNuevo = true;

    /** Cuando esta activo, el aviso va al asesor duenio del prospecto. */
    @Column(name = "notificar_responsable", nullable = false)
    private boolean notificarResponsable = true;

    /** Correos fijos (supervision) separados por coma; complementan al responsable. */
    @Column(name = "correos_copia", length = 1000)
    private String correosCopia;

    /**
     * Minutos de silencio por prospecto y tipo de aviso. Evita que una rafaga de
     * mensajes del mismo cliente llene la bandeja del asesor.
     */
    @Column(name = "cooldown_minutos", nullable = false)
    private int cooldownMinutos = 30;
}
