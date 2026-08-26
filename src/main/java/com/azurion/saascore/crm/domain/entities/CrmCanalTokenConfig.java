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

    public String getCanal() {
        return canal;
    }

    public void setCanal(String canal) {
        this.canal = canal;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getVerifyToken() {
        return verifyToken;
    }

    public void setVerifyToken(String verifyToken) {
        this.verifyToken = verifyToken;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getPhoneNumberId() {
        return phoneNumberId;
    }

    public void setPhoneNumberId(String phoneNumberId) {
        this.phoneNumberId = phoneNumberId;
    }

    public String getWabaId() {
        return wabaId;
    }

    public void setWabaId(String wabaId) {
        this.wabaId = wabaId;
    }

    public OffsetDateTime getWebhookVerifiedAt() {
        return webhookVerifiedAt;
    }

    public void setWebhookVerifiedAt(OffsetDateTime webhookVerifiedAt) {
        this.webhookVerifiedAt = webhookVerifiedAt;
    }

    public OffsetDateTime getLastWebhookAt() {
        return lastWebhookAt;
    }

    public void setLastWebhookAt(OffsetDateTime lastWebhookAt) {
        this.lastWebhookAt = lastWebhookAt;
    }

    public OffsetDateTime getLastInboundMessageAt() {
        return lastInboundMessageAt;
    }

    public void setLastInboundMessageAt(OffsetDateTime lastInboundMessageAt) {
        this.lastInboundMessageAt = lastInboundMessageAt;
    }

    public OffsetDateTime getLastConnectionTestAt() {
        return lastConnectionTestAt;
    }

    public void setLastConnectionTestAt(OffsetDateTime lastConnectionTestAt) {
        this.lastConnectionTestAt = lastConnectionTestAt;
    }

    public Boolean getLastConnectionOk() {
        return lastConnectionOk;
    }

    public void setLastConnectionOk(Boolean lastConnectionOk) {
        this.lastConnectionOk = lastConnectionOk;
    }

    public String getLastConnectionMessage() {
        return lastConnectionMessage;
    }

    public void setLastConnectionMessage(String lastConnectionMessage) {
        this.lastConnectionMessage = lastConnectionMessage;
    }

    public Boolean getWabaSubscribed() {
        return wabaSubscribed;
    }

    public void setWabaSubscribed(Boolean wabaSubscribed) {
        this.wabaSubscribed = wabaSubscribed;
    }

    public String getMetaDisplayPhoneNumber() {
        return metaDisplayPhoneNumber;
    }

    public void setMetaDisplayPhoneNumber(String metaDisplayPhoneNumber) {
        this.metaDisplayPhoneNumber = metaDisplayPhoneNumber;
    }

    public String getMetaVerifiedName() {
        return metaVerifiedName;
    }

    public void setMetaVerifiedName(String metaVerifiedName) {
        this.metaVerifiedName = metaVerifiedName;
    }

    public String getMetaQualityRating() {
        return metaQualityRating;
    }

    public void setMetaQualityRating(String metaQualityRating) {
        this.metaQualityRating = metaQualityRating;
    }

    public OffsetDateTime getMetaTokenExpiresAt() {
        return metaTokenExpiresAt;
    }

    public void setMetaTokenExpiresAt(OffsetDateTime metaTokenExpiresAt) {
        this.metaTokenExpiresAt = metaTokenExpiresAt;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }
}
