package com.azurion.saascore.facturacion.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "azurion.facturador.callback")
public class FacturadorCallbackProperties {

    private boolean enabled = true;
    private String apiKey = "";
    private String secret = "";
    private String headerApiKey = "X-API-Key";
    private String headerSignature = "X-Signature";
    private String headerTimestamp = "X-Timestamp";
    private String headerNonce = "X-Nonce";
    private int timestampToleranceSeconds = 300;
    private int nonceTtlSeconds = 600;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getHeaderApiKey() {
        return headerApiKey;
    }

    public void setHeaderApiKey(String headerApiKey) {
        this.headerApiKey = headerApiKey;
    }

    public String getHeaderSignature() {
        return headerSignature;
    }

    public void setHeaderSignature(String headerSignature) {
        this.headerSignature = headerSignature;
    }

    public String getHeaderTimestamp() {
        return headerTimestamp;
    }

    public void setHeaderTimestamp(String headerTimestamp) {
        this.headerTimestamp = headerTimestamp;
    }

    public String getHeaderNonce() {
        return headerNonce;
    }

    public void setHeaderNonce(String headerNonce) {
        this.headerNonce = headerNonce;
    }

    public int getTimestampToleranceSeconds() {
        return timestampToleranceSeconds;
    }

    public void setTimestampToleranceSeconds(int timestampToleranceSeconds) {
        this.timestampToleranceSeconds = timestampToleranceSeconds;
    }

    public int getNonceTtlSeconds() {
        return nonceTtlSeconds;
    }

    public void setNonceTtlSeconds(int nonceTtlSeconds) {
        this.nonceTtlSeconds = nonceTtlSeconds;
    }
}

