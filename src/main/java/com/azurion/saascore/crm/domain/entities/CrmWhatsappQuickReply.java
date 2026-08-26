package com.azurion.saascore.crm.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "crm_whatsapp_quick_replies")
public class CrmWhatsappQuickReply extends BaseEntity {

    @Column(name = "usuario_id", nullable = false, length = 120)
    private String usuarioId;

    @Column(name = "slot", nullable = false)
    private Integer slot;

    @Column(name = "titulo", nullable = false, length = 80)
    private String titulo;

    @Column(name = "mensaje", nullable = false, columnDefinition = "TEXT")
    private String mensaje;
}
