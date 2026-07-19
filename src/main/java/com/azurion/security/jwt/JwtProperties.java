package com.azurion.security.jwt;

import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import jakarta.annotation.PostConstruct;
import io.jsonwebtoken.io.Decoders;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    private static final Set<String> FORBIDDEN_SECRETS = Set.of(
            "replace_this_with_very_long_secure_secret_32_chars_minimum",
            "change-me",
            "secret"
    );

    private String secret;
    private String issuer;
    private long expirationMinutes;

    public Duration expiration() {
        return Duration.ofMinutes(expirationMinutes);
    }

    @PostConstruct
    void validate() {
        if (secret == null || secret.isBlank() || FORBIDDEN_SECRETS.contains(secret.trim().toLowerCase())) {
            throw new IllegalStateException("JWT_SECRET debe configurarse con una clave aleatoria segura.");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secret.matches("^[A-Za-z0-9+/=]+$") && secret.length() >= 44) {
            try {
                keyBytes = Decoders.BASE64.decode(secret);
            } catch (RuntimeException ignored) {
                keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            }
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe contener al menos 32 bytes de entropia.");
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalStateException("JWT_ISSUER no puede estar vacio.");
        }
        if (expirationMinutes < 1 || expirationMinutes > 10_080) {
            throw new IllegalStateException("JWT_EXP_MIN debe estar entre 1 minuto y 7 dias.");
        }
    }
}
