package com.azurion.saascore.facturacion.infrastructure.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "azurion.facturador")
public class FacturadorProperties {

    private String baseUrl = "http://127.0.0.1:8000";
    /** URL publica usada por Laravel al firmar enlaces de artefactos. */
    private String artifactBaseUrl;
    private String apiPrefix = "/api";
    private long connectTimeoutMillis = 5000;
    private long readTimeoutMillis = 20000;
    private boolean waitProcessedEnabled = false;
    private long waitProcessedTimeoutMillis = 90000;
    private long waitProcessedPollIntervalMillis = 1500;
    private boolean provisioningEnabled = true;
    private String clientId = "azurion-core";
    private String clientSecret;
    private String signatureVersion = "v2";
    private boolean allowLegacyApiKey = false;
    /**
     * Compatibilidad temporal con instalaciones anteriores. Las nuevas
     * instalaciones deben usar clientId + clientSecret.
     */
    private String defaultApiKey;
    private Map<String, String> tenantApiKeys = new HashMap<>();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getArtifactBaseUrl() {
        return artifactBaseUrl;
    }

    public void setArtifactBaseUrl(String artifactBaseUrl) {
        this.artifactBaseUrl = artifactBaseUrl;
    }

    public String getApiPrefix() {
        return apiPrefix;
    }

    public void setApiPrefix(String apiPrefix) {
        this.apiPrefix = apiPrefix;
    }

    public long getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(long connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public long getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public void setReadTimeoutMillis(long readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
    }

    public boolean isWaitProcessedEnabled() {
        return waitProcessedEnabled;
    }

    public void setWaitProcessedEnabled(boolean waitProcessedEnabled) {
        this.waitProcessedEnabled = waitProcessedEnabled;
    }

    public long getWaitProcessedTimeoutMillis() {
        return waitProcessedTimeoutMillis;
    }

    public void setWaitProcessedTimeoutMillis(long waitProcessedTimeoutMillis) {
        this.waitProcessedTimeoutMillis = waitProcessedTimeoutMillis;
    }

    public long getWaitProcessedPollIntervalMillis() {
        return waitProcessedPollIntervalMillis;
    }

    public void setWaitProcessedPollIntervalMillis(long waitProcessedPollIntervalMillis) {
        this.waitProcessedPollIntervalMillis = waitProcessedPollIntervalMillis;
    }

    public boolean isProvisioningEnabled() {
        return provisioningEnabled;
    }

    public void setProvisioningEnabled(boolean provisioningEnabled) {
        this.provisioningEnabled = provisioningEnabled;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getSignatureVersion() {
        return signatureVersion;
    }

    public void setSignatureVersion(String signatureVersion) {
        this.signatureVersion = signatureVersion;
    }

    public boolean isAllowLegacyApiKey() {
        return allowLegacyApiKey;
    }

    public void setAllowLegacyApiKey(boolean allowLegacyApiKey) {
        this.allowLegacyApiKey = allowLegacyApiKey;
    }

    public String getDefaultApiKey() {
        return defaultApiKey;
    }

    public void setDefaultApiKey(String defaultApiKey) {
        this.defaultApiKey = defaultApiKey;
    }

    public Map<String, String> getTenantApiKeys() {
        return tenantApiKeys;
    }

    public void setTenantApiKeys(Map<String, String> tenantApiKeys) {
        this.tenantApiKeys = tenantApiKeys;
    }

    public Optional<FacturadorCredential> resolveCredential(String tenantId) {
        if (allowLegacyApiKey && tenantId != null) {
            String tenantKey = tenantApiKeys.get(tenantId);
            if (tenantKey != null && !tenantKey.isBlank()) {
                return Optional.of(FacturadorCredential.legacy(tenantKey.trim()));
            }
        }
        if (clientSecret != null && !clientSecret.isBlank()) {
            String resolvedClientId = clientId == null || clientId.isBlank()
                    ? "azurion-core"
                    : clientId.trim();
            return Optional.of(new FacturadorCredential(
                    resolvedClientId,
                    clientSecret.trim(),
                    normalizeSignatureVersion(signatureVersion),
                    false
            ));
        }
        if (allowLegacyApiKey && defaultApiKey != null && !defaultApiKey.isBlank()) {
            return Optional.of(FacturadorCredential.legacy(defaultApiKey.trim()));
        }
        return Optional.empty();
    }

    private String normalizeSignatureVersion(String value) {
        return "v2".equalsIgnoreCase(value == null ? "" : value.trim()) ? "v2" : "v1";
    }

    public record FacturadorCredential(
            String clientId,
            String secret,
            String signatureVersion,
            boolean legacyApiKey
    ) {
        public static FacturadorCredential legacy(String apiKey) {
            return new FacturadorCredential("", apiKey, "v1", true);
        }
    }
}
