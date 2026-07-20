package com.azurion.saascore.crm.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
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

    @Column(name = "app_secret", length = 1000)
    private String appSecret;

    @Column(name = "phone_number_id", length = 180)
    private String phoneNumberId;

    @Column(name = "waba_id", length = 180)
    private String wabaId;

    @Column(name = "webhook_verified_at")
    private OffsetDateTime webhookVerifiedAt;

    @Column(name = "last_webhook_at")
    private OffsetDateTime lastWebhookAt;

    @Column(name = "last_inbound_message_at")
    private OffsetDateTime lastInboundMessageAt;

    @Column(name = "last_connection_test_at")
    private OffsetDateTime lastConnectionTestAt;

    @Column(name = "last_connection_ok")
    private Boolean lastConnectionOk;

    @Column(name = "last_connection_message", length = 500)
    private String lastConnectionMessage;

    @Column(name = "waba_subscribed")
    private Boolean wabaSubscribed;

    @Column(name = "meta_display_phone_number", length = 80)
    private String metaDisplayPhoneNumber;

    @Column(name = "meta_verified_name", length = 180)
    private String metaVerifiedName;

    @Column(name = "meta_quality_rating", length = 40)
    private String metaQualityRating;

    @Column(name = "meta_token_expires_at")
    private OffsetDateTime metaTokenExpiresAt;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;
}
