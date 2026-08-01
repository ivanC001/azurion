package com.azurion.saascore.facturacion.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class FacturadorHmacSigner {

    public String sign(
            String version,
            String method,
            String requestUri,
            String timestamp,
            String nonce,
            String tenantId,
            String tenantRuc,
            String body,
            String secret
    ) {
        String canonical = buildCanonical(
                version,
                method,
                requestUri,
                timestamp,
                nonce,
                tenantId,
                tenantRuc,
                sha256Hex(body == null ? "" : body)
        );
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(
                    mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo firmar la solicitud al facturador.", exception);
        }
    }

    String buildCanonical(
            String version,
            String method,
            String requestUri,
            String timestamp,
            String nonce,
            String tenantId,
            String tenantRuc,
            String bodyHash
    ) {
        String normalizedMethod = method == null ? "GET" : method.trim().toUpperCase(Locale.ROOT);
        if ("v2".equalsIgnoreCase(version)) {
            return String.join("\n",
                    "v2",
                    normalizedMethod,
                    normalize(requestUri),
                    normalize(timestamp),
                    normalize(nonce),
                    normalize(tenantId),
                    normalize(tenantRuc),
                    normalize(bodyHash)
            );
        }
        return String.join("\n",
                normalizedMethod,
                normalize(requestUri),
                normalize(timestamp),
                normalize(nonce),
                normalize(bodyHash)
        );
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(hash.length * 2);
            for (byte item : hash) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo calcular el hash de la solicitud.", exception);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
