package com.azurion.saascore.settings.email.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "azurion.email.transport")
public class EmailTransportProperties {

    private int connectTimeoutMillis = 10_000;
    private int readTimeoutMillis = 20_000;
    private int writeTimeoutMillis = 20_000;
    private String tlsProtocols = "TLSv1.2 TLSv1.3";
    private boolean checkServerIdentity = true;
}
