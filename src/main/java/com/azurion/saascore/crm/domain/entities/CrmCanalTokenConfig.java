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
@Table(name = "crm_canal_token_config")
public class CrmCanalTokenConfig extends BaseEntity {

    @Column(name = "canal", nullable = false, unique = true, length = 40)
    private String canal;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "access_token", length = 2000)
    private String accessToken;

    @Column(name = "verify_token", length = 300)
    private String verifyToken;

    @Column(name = "webhook_url", length = 500)
    private String webhookUrl;

    @Column(name = "app_id", length = 180)
    private String appId;

    @Column(name = "phone_number_id", length = 180)
    private String phoneNumberId;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;
}
