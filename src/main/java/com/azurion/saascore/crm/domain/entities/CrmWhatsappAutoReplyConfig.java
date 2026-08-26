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
@Table(name = "crm_whatsapp_auto_reply_config")
public class CrmWhatsappAutoReplyConfig extends BaseEntity {

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "modo", nullable = false, length = 20)
    private String modo = "SIEMPRE";

    @Column(name = "mensaje", columnDefinition = "TEXT")
    private String mensaje;

    @Column(name = "cooldown_minutos", nullable = false)
    private Integer cooldownMinutos = 720;
}
