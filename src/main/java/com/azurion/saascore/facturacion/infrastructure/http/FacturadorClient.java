package com.azurion.saascore.facturacion.infrastructure.http;

import com.azurion.saascore.facturacion.infrastructure.config.FacturadorProperties;
import com.azurion.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FacturadorClient {

    private static final long MIN_WAIT_PROCESSED_TIMEOUT_MS = 5_000;
    private static final long MAX_WAIT_PROCESSED_TIMEOUT_MS = 300_000;
    private static final long MIN_WAIT_PROCESSED_POLL_INTERVAL_MS = 250;
    private static final long MAX_WAIT_PROCESSED_POLL_INTERVAL_MS = 5_000;
    private static final long MAX_LIST_STATUS_TIMEOUT_MS = 4500;

    private static final String HEADER_API_KEY = "X-API-Key";
    private static final String HEADER_TIMESTAMP = "X-Timestamp";
    private static final String HEADER_NONCE = "X-Nonce";
    private static final String HEADER_SIGNATURE = "X-Signature";

    private static final Set<String> TERMINAL_STATES = Set.of("ACEPTADO", "RECHAZADO", "ERROR");

    private final FacturadorProperties properties;
    private final ObjectMapper objectMapper;

    public FacturadorEmissionResult emitirDocumento(String tenantId, String tenantRuc, String endpointPath, Object payload, String tipoComprobante) {
        String apiKey = properties.resolveApiKey(tenantId)
                .orElseThrow(() -> new BusinessException(
                        "FACTURADOR_API_KEY_MISSING",
                        "No existe API key configurada para facturador en el tenant " + tenantId
                ));

        String path = normalizePath(endpointPath);

        try {
            String body = objectMapper.writeValueAsString(payload);
            SignedCallResult initial = executeSignedRequest("POST", path, body, apiKey, tenantRuc);

            if (!isSuccessfulResponse(initial.status(), initial.body())) {
                throw new BusinessException("FACTURADOR_ERROR", resolveMessage(initial.body(), initial.status()));
            }

            JsonNode finalBody = initial.body();
            int finalStatus = initial.status();
            String finalMessage = resolveMessage(finalBody, finalStatus);

            if (shouldWaitForProcessed(tipoComprobante, finalBody)) {
                SignedCallResult processed = waitForProcessed(apiKey, tenantRuc, finalBody);
                if (processed != null) {
                    finalBody = processed.body();
                    finalStatus = processed.status();
                    finalMessage = resolveProcessedMessage(processed.body(), processed.status());
                } else {
                    finalMessage = finalMessage + " (pendiente de procesamiento SUNAT)";
                }
            }

            return new FacturadorEmissionResult(
                    true,
                    finalStatus,
                    path,
                    tipoComprobante,
                    finalMessage,
                    finalBody
            );
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("FACTURADOR_ERROR", "No se pudo conectar con facturador: " + ex.getMessage());
        }
    }

    public Map<String, FacturadorDocumentoStatusResult> consultarDocumentosPorExternalIds(
            String tenantId,
            String tenantRuc,
            List<String> externalIds
    ) {
        List<String> normalizedIds = normalizeExternalIds(externalIds);
        if (normalizedIds.isEmpty()) {
            return Map.of();
        }

        String apiKey = properties.resolveApiKey(tenantId)
                .orElseThrow(() -> new BusinessException(
                        "FACTURADOR_API_KEY_MISSING",
                        "No existe API key configurada para facturador en el tenant " + tenantId
                ));

        Map<String, FacturadorDocumentoStatusResult> results = new LinkedHashMap<>();
        int chunkSize = 50;

        for (int start = 0; start < normalizedIds.size(); start += chunkSize) {
            int end = Math.min(start + chunkSize, normalizedIds.size());
            List<String> chunk = normalizedIds.subList(start, end);

            String csv = String.join(",", chunk);
            String requestPath = normalizePath("/documentos")
                    + "?limit=" + chunk.size()
                    + "&external_ids=" + encodeQueryValue(csv);

            try {
                long timeoutMs = Math.min(properties.getReadTimeoutMillis(), MAX_LIST_STATUS_TIMEOUT_MS);
                SignedCallResult response = executeSignedRequest("GET", requestPath, "", apiKey, tenantRuc, timeoutMs);

                if (!isSuccessfulResponse(response.status(), response.body())) {
                    throw new BusinessException("FACTURADOR_STATUS_ERROR", resolveMessage(response.body(), response.status()));
                }

                mergeStatusItems(results, response);
            } catch (BusinessException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new BusinessException("FACTURADOR_STATUS_ERROR", "No se pudo consultar estados en facturador: " + exception.getMessage());
            }
        }

        return results;
    }

    private SignedCallResult waitForProcessed(String apiKey, String tenantRuc, JsonNode initialBody) throws Exception {
        Long documentoId = extractDocumentoId(initialBody);
        if (documentoId == null) {
            return null;
        }

        String statusPath = normalizePath("/sunat/estado") + "?documento_id=" + documentoId;
        long timeoutMs = Math.max(MIN_WAIT_PROCESSED_TIMEOUT_MS, properties.getWaitProcessedTimeoutMillis());
        timeoutMs = Math.min(timeoutMs, MAX_WAIT_PROCESSED_TIMEOUT_MS);

        long pollInterval = Math.max(MIN_WAIT_PROCESSED_POLL_INTERVAL_MS, properties.getWaitProcessedPollIntervalMillis());
        pollInterval = Math.min(pollInterval, MAX_WAIT_PROCESSED_POLL_INTERVAL_MS);
        if (pollInterval >= timeoutMs) {
            pollInterval = Math.max(MIN_WAIT_PROCESSED_POLL_INTERVAL_MS, timeoutMs / 4);
        }
        long deadlineEpochMillis = System.currentTimeMillis() + timeoutMs;

        SignedCallResult lastSuccessful = null;

        while (System.currentTimeMillis() <= deadlineEpochMillis) {
            SignedCallResult statusCall = executeSignedRequest("GET", statusPath, "", apiKey, tenantRuc);
            if (!isSuccessfulResponse(statusCall.status(), statusCall.body())) {
                throw new BusinessException("FACTURADOR_STATUS_ERROR", resolveMessage(statusCall.body(), statusCall.status()));
            }

            lastSuccessful = statusCall;
            String estado = extractEstado(statusCall.body());

            if (TERMINAL_STATES.contains(estado)) {
                return statusCall;
            }

            try {
                Thread.sleep(pollInterval);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                throw new BusinessException("FACTURADOR_WAIT_INTERRUPTED", "La espera de procesamiento SUNAT fue interrumpida.");
            }
        }

        return lastSuccessful;
    }

    private boolean shouldWaitForProcessed(String tipoComprobante, JsonNode initialBody) {
        if (!properties.isWaitProcessedEnabled()) {
            return false;
        }

        String tipo = tipoComprobante == null ? "" : tipoComprobante.trim().toUpperCase(Locale.ROOT);
        if ("TICKET_VENTA".equals(tipo)
                || "GUIA_REMISION".equals(tipo)
                || "NOTA_CREDITO".equals(tipo)
                || "NOTA_DEBITO".equals(tipo)
                || "07".equals(tipo)
                || "08".equals(tipo)
                || "09".equals(tipo)) {
            return false;
        }

        Long documentoId = extractDocumentoId(initialBody);
        if (documentoId == null) {
            return false;
        }

        JsonNode data = extractData(initialBody);
        if (data != null && data.has("sunat_async") && !data.path("sunat_async").asBoolean(true)) {
            return false;
        }

        return true;
    }

    private List<String> normalizeExternalIds(List<String> externalIds) {
        if (externalIds == null || externalIds.isEmpty()) {
            return List.of();
        }

        List<String> normalized = new ArrayList<>();
        for (String externalId : externalIds) {
            if (externalId == null) {
                continue;
            }
            String trimmed = externalId.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            if (!normalized.contains(trimmed)) {
                normalized.add(trimmed);
            }
            if (normalized.size() >= 200) {
                break;
            }
        }
        return normalized;
    }

    private void mergeStatusItems(Map<String, FacturadorDocumentoStatusResult> target, SignedCallResult response) {
        JsonNode body = response.body();
        JsonNode data = extractData(body);
        if (data == null || data.isNull() || data.isMissingNode()) {
            return;
        }

        JsonNode items = data.path("items");
        if (items == null || items.isNull() || items.isMissingNode()) {
            return;
        }
        if (!items.isArray()) {
            return;
        }

        for (JsonNode item : items) {
            String externalId = text(item, "external_id");
            if (externalId == null || externalId.isBlank()) {
                continue;
            }

            FacturadorDocumentoStatusResult result = new FacturadorDocumentoStatusResult(
                    externalId,
                    integer(item, "id"),
                    text(item, "tipo_documento"),
                    text(item, "serie"),
                    text(item, "correlativo"),
                    text(item, "estado"),
                    text(item, "sunat_estado"),
                    text(item, "sunat_mensaje"),
                    text(item, "sunat_codigo_error"),
                    text(item, "ticket"),
                    text(item, "hash"),
                    text(item, "pdf_url"),
                    text(item, "xml_url"),
                    text(item, "cdr_url"),
                    response.status(),
                    item
            );
            target.put(externalId, result);
        }
    }

    private String encodeQueryValue(String value) {
        if (value == null) {
            return "";
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String text(JsonNode source, String fieldName) {
        if (source == null || source.isNull() || source.isMissingNode()) {
            return null;
        }
        JsonNode node = source.path(fieldName);
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.asText("").trim();
        return value.isBlank() ? null : value;
    }

    private Integer integer(JsonNode source, String fieldName) {
        if (source == null || source.isNull() || source.isMissingNode()) {
            return null;
        }
        JsonNode node = source.path(fieldName);
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.canConvertToInt()) {
            return node.asInt();
        }
        return null;
    }

    private SignedCallResult executeSignedRequest(
            String method,
            String requestUri,
            String requestBody,
            String apiKey,
            String tenantRuc
    ) throws Exception {
        return executeSignedRequest(
                method,
                requestUri,
                requestBody,
                apiKey,
                tenantRuc,
                properties.getReadTimeoutMillis()
        );
    }

    private SignedCallResult executeSignedRequest(
            String method,
            String requestUri,
            String requestBody,
            String apiKey,
            String tenantRuc,
            long requestTimeoutMs
    ) throws Exception {
        String normalizedMethod = method == null ? "GET" : method.trim().toUpperCase(Locale.ROOT);
        String baseUrl = properties.getBaseUrl().replaceAll("/+$", "");
        URI endpoint = URI.create(baseUrl + requestUri);

        String body = requestBody == null ? "" : requestBody;
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String canonical = buildCanonical(normalizedMethod, requestUri, timestamp, nonce, sha256Hex(body));
        String signature = signBase64(canonical, apiKey);

        HttpRequest.Builder builder = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofMillis(Math.max(500, requestTimeoutMs)))
                .header("Accept", "application/json")
                .header(HEADER_API_KEY, apiKey)
                .header("X-Tenant-RUC", tenantRuc)
                .header(HEADER_TIMESTAMP, timestamp)
                .header(HEADER_NONCE, nonce)
                .header(HEADER_SIGNATURE, signature);

        if ("POST".equals(normalizedMethod)) {
            builder.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        } else {
            builder.GET();
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMillis()))
                .build();

        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new SignedCallResult(response.statusCode(), parseBody(response.body()));
    }

    private String normalizePath(String endpointPath) {
        String prefix = properties.getApiPrefix() == null ? "/api" : properties.getApiPrefix().trim();
        if (prefix.isBlank()) {
            prefix = "/api";
        }
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        prefix = prefix.replaceAll("/+$", "");

        String path = endpointPath == null ? "" : endpointPath.trim();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        if (path.startsWith(prefix + "/") || path.equals(prefix)) {
            return path;
        }
        return prefix + path;
    }

    private String buildCanonical(String method, String requestUri, String timestamp, String nonce, String bodyHash) {
        return String.join("\n",
                method.toUpperCase(Locale.ROOT),
                requestUri,
                timestamp,
                nonce,
                bodyHash
        );
    }

    private String signBase64(String canonical, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signature = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature);
    }

    private String sha256Hex(String body) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(body.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private JsonNode parseBody(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(body);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isSuccessfulResponse(int status, JsonNode json) {
        return status >= 200 && status < 300 && (json == null || json.path("success").asBoolean(true));
    }

    private String resolveMessage(JsonNode json, int status) {
        if (json == null) {
            return "Facturador respondio sin contenido (" + status + ")";
        }
        String backendMessage = json.path("message").asText("");
        if (!backendMessage.isBlank()) {
            return backendMessage;
        }
        if (json.path("success").asBoolean(false)) {
            return "Documento enviado al facturador";
        }
        return "Facturador rechazo la solicitud (" + status + ")";
    }

    private String resolveProcessedMessage(JsonNode json, int status) {
        JsonNode data = extractData(json);
        String estado = data == null ? "" : data.path("estado").asText("");
        if (!estado.isBlank()) {
            return "Documento procesado por SUNAT con estado: " + estado;
        }
        return resolveMessage(json, status);
    }

    private JsonNode extractData(JsonNode root) {
        if (root == null || root.isMissingNode() || root.isNull()) {
            return null;
        }
        JsonNode data = root.path("data");
        if (data.isMissingNode() || data.isNull()) {
            return null;
        }
        return data;
    }

    private Long extractDocumentoId(JsonNode root) {
        JsonNode data = extractData(root);
        if (data == null || !data.has("documento_id") || !data.path("documento_id").canConvertToLong()) {
            return null;
        }
        return data.path("documento_id").asLong();
    }

    private String extractEstado(JsonNode root) {
        JsonNode data = extractData(root);
        if (data == null) {
            return "";
        }
        return data.path("estado").asText("").trim().toUpperCase(Locale.ROOT);
    }

    private record SignedCallResult(int status, JsonNode body) {
    }

    public record FacturadorEmissionResult(
            boolean success,
            int status,
            String endpoint,
            String tipoComprobante,
            String message,
            JsonNode responseBody
    ) {
    }

    public record FacturadorDocumentoStatusResult(
            String externalId,
            Integer documentoId,
            String tipoDocumento,
            String serie,
            String correlativo,
            String estadoInterno,
            String sunatEstado,
            String sunatMensaje,
            String sunatCodigoError,
            String ticket,
            String hash,
            String pdfUrl,
            String xmlUrl,
            String cdrUrl,
            int httpStatus,
            JsonNode rawData
    ) {
    }
}
