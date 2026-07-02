package com.azurion.saascore.facturacion.infrastructure.security;

import com.azurion.saascore.facturacion.infrastructure.config.FacturadorCallbackProperties;
import com.azurion.shared.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FacturadorCallbackVerifier {

    private static final Pattern NONCE_PATTERN = Pattern.compile("^[a-zA-Z0-9._:-]{8,120}$");
    private static final int MAX_NONCE_CACHE_SIZE = 10_000;

    private final FacturadorCallbackProperties properties;
    private final Map<String, Long> usedNoncesByKey = new ConcurrentHashMap<>();

    public void verify(HttpServletRequest request, String rawBody) {
        if (!properties.isEnabled()) {
            return;
        }

        String configuredApiKey = normalize(properties.getApiKey());
        String configuredSecret = normalize(properties.getSecret());
        if (configuredApiKey == null || configuredSecret == null) {
            throw new BusinessException("FACTURADOR_CALLBACK_CONFIG_ERROR", "Callback de facturador no esta configurado correctamente.");
        }

        String headerApiKey = normalizeHeader(properties.getHeaderApiKey(), "X-API-Key");
        String headerSignature = normalizeHeader(properties.getHeaderSignature(), "X-Signature");
        String headerTimestamp = normalizeHeader(properties.getHeaderTimestamp(), "X-Timestamp");
        String headerNonce = normalizeHeader(properties.getHeaderNonce(), "X-Nonce");

        String providedApiKey = normalize(request.getHeader(headerApiKey));
        String providedSignature = normalize(request.getHeader(headerSignature));
        String providedTimestamp = normalize(request.getHeader(headerTimestamp));
        String providedNonce = normalize(request.getHeader(headerNonce));

        if (providedApiKey == null || providedSignature == null || providedTimestamp == null || providedNonce == null) {
            throw new BusinessException("FACTURADOR_CALLBACK_HEADERS_MISSING", "Faltan headers de autenticacion para callback de facturador.");
        }

        if (!constantEquals(configuredApiKey, providedApiKey)) {
            throw new BusinessException("FACTURADOR_CALLBACK_API_KEY_INVALID", "API key invalida para callback de facturador.");
        }

        long timestampSeconds = parseTimestampSeconds(providedTimestamp);
        long nowSeconds = Instant.now().getEpochSecond();
        int tolerance = Math.max(10, properties.getTimestampToleranceSeconds());
        if (Math.abs(nowSeconds - timestampSeconds) > tolerance) {
            throw new BusinessException("FACTURADOR_CALLBACK_TIMESTAMP_EXPIRED", "Timestamp de callback fuera de rango.");
        }

        if (!NONCE_PATTERN.matcher(providedNonce).matches()) {
            throw new BusinessException("FACTURADOR_CALLBACK_NONCE_INVALID", "Nonce invalido para callback de facturador.");
        }

        String requestUri = resolveRequestUri(request);
        String body = rawBody == null ? "" : rawBody;

        String canonical = buildCanonical(
                request.getMethod(),
                requestUri,
                providedTimestamp,
                providedNonce,
                sha256Hex(body)
        );

        String expectedBase64 = signBase64(canonical, configuredSecret);
        String expectedHex = signHex(canonical, configuredSecret);

        boolean valid = constantEquals(expectedBase64, providedSignature)
                || constantEquals(expectedHex, providedSignature.toLowerCase());

        if (!valid) {
            throw new BusinessException("FACTURADOR_CALLBACK_SIGNATURE_INVALID", "Firma HMAC invalida para callback de facturador.");
        }

        assertFreshNonce(configuredApiKey, providedNonce, timestampSeconds, nowSeconds);
    }

    private void assertFreshNonce(String apiKey, String nonce, long timestampSeconds, long nowSeconds) {
        clearExpiredNonces(nowSeconds);
        if (usedNoncesByKey.size() > MAX_NONCE_CACHE_SIZE) {
            usedNoncesByKey.clear();
        }

        int nonceTtl = Math.max(30, properties.getNonceTtlSeconds());
        long expiresAt = nowSeconds + nonceTtl;
        String nonceKey = apiKey + ":" + nonce + ":" + timestampSeconds;

        Long previous = usedNoncesByKey.putIfAbsent(nonceKey, expiresAt);
        if (previous != null && previous >= nowSeconds) {
            throw new BusinessException("FACTURADOR_CALLBACK_REPLAY", "Callback duplicado detectado (nonce ya utilizado).");
        }
    }

    private void clearExpiredNonces(long nowSeconds) {
        usedNoncesByKey.entrySet().removeIf(entry -> entry.getValue() < nowSeconds);
    }

    private String resolveRequestUri(HttpServletRequest request) {
        String path = request.getRequestURI();
        String query = request.getQueryString();
        if (query == null || query.isBlank()) {
            return path;
        }
        return path + "?" + query;
    }

    private long parseTimestampSeconds(String raw) {
        if (!raw.matches("^\\d{10,13}$")) {
            throw new BusinessException("FACTURADOR_CALLBACK_TIMESTAMP_INVALID", "Timestamp de callback invalido.");
        }
        long numeric = Long.parseLong(raw);
        if (raw.length() == 13) {
            numeric = numeric / 1000L;
        }
        if (numeric <= 0) {
            throw new BusinessException("FACTURADOR_CALLBACK_TIMESTAMP_INVALID", "Timestamp de callback invalido.");
        }
        return numeric;
    }

    private String buildCanonical(String method, String requestUri, String timestamp, String nonce, String bodyHash) {
        return String.join("\n",
                method == null ? "POST" : method.trim().toUpperCase(),
                requestUri == null ? "" : requestUri,
                timestamp,
                nonce,
                bodyHash
        );
    }

    private String sha256Hex(String body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(body.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception exception) {
            throw new BusinessException("FACTURADOR_CALLBACK_HASH_ERROR", "No se pudo calcular hash del callback.");
        }
    }

    private String signBase64(String canonical, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signature = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature);
        } catch (Exception exception) {
            throw new BusinessException("FACTURADOR_CALLBACK_SIGN_ERROR", "No se pudo validar la firma del callback.");
        }
    }

    private String signHex(String canonical, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signature = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(signature.length * 2);
            for (byte b : signature) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception exception) {
            throw new BusinessException("FACTURADOR_CALLBACK_SIGN_ERROR", "No se pudo validar la firma del callback.");
        }
    }

    private boolean constantEquals(String expected, String provided) {
        if (expected == null || provided == null) {
            return false;
        }
        byte[] left = expected.getBytes(StandardCharsets.UTF_8);
        byte[] right = provided.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(left, right);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String normalizeHeader(String configured, String fallback) {
        String normalized = normalize(configured);
        return normalized == null ? fallback : normalized;
    }
}
