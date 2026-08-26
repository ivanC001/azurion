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

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public void setReadTimeoutMillis(int readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
    }

    public int getWriteTimeoutMillis() {
        return writeTimeoutMillis;
    }

    public void setWriteTimeoutMillis(int writeTimeoutMillis) {
        this.writeTimeoutMillis = writeTimeoutMillis;
    }

    public String getTlsProtocols() {
        return tlsProtocols;
    }

    public void setTlsProtocols(String tlsProtocols) {
        this.tlsProtocols = tlsProtocols;
    }

    public boolean isCheckServerIdentity() {
        return checkServerIdentity;
    }

    public void setCheckServerIdentity(boolean checkServerIdentity) {
        this.checkServerIdentity = checkServerIdentity;
    }
}
