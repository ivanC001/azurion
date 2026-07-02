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
}

