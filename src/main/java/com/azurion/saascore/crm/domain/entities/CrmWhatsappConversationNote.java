package com.azurion.saascore.crm.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "crm_whatsapp_conversation_notes")
public class CrmWhatsappConversationNote extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private CrmWhatsappConversation conversation;

    @Column(name = "slot", nullable = false)
    private Integer slot;

    @Column(name = "contenido", nullable = false, columnDefinition = "TEXT")
    private String contenido;
}
