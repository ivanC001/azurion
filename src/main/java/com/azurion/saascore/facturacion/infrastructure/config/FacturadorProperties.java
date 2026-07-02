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
    private String apiPrefix = "/api";
    private long connectTimeoutMillis = 5000;
    private long readTimeoutMillis = 20000;
    private boolean waitProcessedEnabled = true;
    private long waitProcessedTimeoutMillis = 90000;
    private long waitProcessedPollIntervalMillis = 1500;
    private String defaultApiKey;
    private Map<String, String> tenantApiKeys = new HashMap<>();

    public Optional<String> resolveApiKey(String tenantId) {
        if (tenantId != null) {
            String tenantKey = tenantApiKeys.get(tenantId);
            if (tenantKey != null && !tenantKey.isBlank()) {
                return Optional.of(tenantKey.trim());
            }
        }
        if (defaultApiKey != null && !defaultApiKey.isBlank()) {
            return Optional.of(defaultApiKey.trim());
        }
        return Optional.empty();
    }
}
