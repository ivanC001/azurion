package com.azurion.saascore.crm.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "crm_whatsapp_messages")
public class CrmWhatsappMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prospecto_id")
    private CrmProspecto prospecto;

    @Column(name = "meta_message_id", nullable = false, unique = true, length = 255)
    private String metaMessageId;

    @Column(name = "direccion", nullable = false, length = 15)
    private String direccion;

    @Column(name = "remitente", length = 80)
    private String remitente;

    @Column(name = "destinatario", length = 80)
    private String destinatario;

    @Column(name = "tipo_mensaje", nullable = false, length = 40)
    private String tipoMensaje;

    @Column(name = "contenido", columnDefinition = "TEXT")
    private String contenido;

    @Column(name = "estado", nullable = false, length = 30)
    private String estado;

    @Column(name = "mensaje_en")
    private OffsetDateTime mensajeEn;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "leido_en")
    private OffsetDateTime leidoEn;
}
