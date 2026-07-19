package com.azurion.saascore.crm.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "crm_whatsapp_conversations")
public class CrmWhatsappConversation extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prospecto_id", nullable = false, unique = true)
    private CrmProspecto prospecto;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "ABIERTA";

    @Column(name = "responsable_id", length = 80)
    private String responsableId;

    @Column(name = "no_leidos", nullable = false)
    private Integer noLeidos = 0;

    @Column(name = "ultimo_mensaje", columnDefinition = "TEXT")
    private String ultimoMensaje;

    @Column(name = "ultima_direccion", length = 15)
    private String ultimaDireccion;

    @Column(name = "ultimo_mensaje_en")
    private OffsetDateTime ultimoMensajeEn;

    @Column(name = "nota_interna", columnDefinition = "TEXT")
    private String notaInterna;
}
