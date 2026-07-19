package com.azurion.saascore.crm.infrastructure.http;

import com.azurion.saascore.crm.application.services.CrmSecretEncryptionService;
import com.azurion.saascore.crm.domain.entities.CrmCanalTokenConfig;
import com.azurion.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class WhatsappCloudApiClient {

    private final ObjectMapper objectMapper;
    private final CrmSecretEncryptionService secretEncryptionService;
    private final HttpClient httpClient;
    private final String graphBaseUrl;
    private final String graphApiVersion;
    private final Duration readTimeout;

    public WhatsappCloudApiClient(
            ObjectMapper objectMapper,
            CrmSecretEncryptionService secretEncryptionService,
            @Value("${azurion.whatsapp.graph-base-url}") String graphBaseUrl,
            @Value("${azurion.whatsapp.graph-api-version}") String graphApiVersion,
            @Value("${azurion.whatsapp.connect-timeout-millis}") long connectTimeoutMillis,
            @Value("${azurion.whatsapp.read-timeout-millis}") long readTimeoutMillis) {
        this.objectMapper = objectMapper;
        this.secretEncryptionService = secretEncryptionService;
        this.graphBaseUrl = stripTrailingSlash(graphBaseUrl);
        this.graphApiVersion = validateVersion(graphApiVersion);
        this.readTimeout = Duration.ofMillis(readTimeoutMillis);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMillis))
                .build();
    }

    public SendResult sendText(CrmCanalTokenConfig config, String recipient, String message, boolean previewUrl) {
        String accessToken = secretEncryptionService.decrypt(config.getAccessToken());
        if (!hasText(accessToken) || !hasText(config.getPhoneNumberId())) {
            throw new BusinessException(
                    "CRM_WHATSAPP_CONFIG_INCOMPLETA",
                    "La integracion de WhatsApp no tiene access token o Phone number ID"
            );
        }

        String phoneNumberId = validatePathSegment(config.getPhoneNumberId(), "Phone number ID");
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", recipient);
        payload.put("type", "text");
        ObjectNode text = payload.putObject("text");
        text.put("preview_url", previewUrl);
        text.put("body", message);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(graphBaseUrl + "/" + graphApiVersion + "/" + phoneNumberId + "/messages"))
                    .timeout(readTimeout)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode responseJson = parseResponse(response.body());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String metaMessage = metaError(responseJson, "Meta rechazo el mensaje")
                        + " (HTTP " + response.statusCode() + ")";
                throw new BusinessException("CRM_WHATSAPP_META_ERROR", metaMessage);
            }

            String messageId = responseJson.path("messages").path(0).path("id").asText(null);
            if (!hasText(messageId)) {
                throw new BusinessException("CRM_WHATSAPP_RESPUESTA_INVALIDA", "Meta no devolvio el identificador wamid");
            }
            String whatsappId = responseJson.path("contacts").path(0).path("wa_id").asText(recipient);
            return new SendResult(messageId, whatsappId, response.body());
        } catch (BusinessException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException("CRM_WHATSAPP_ENVIO_INTERRUMPIDO", "El envio a WhatsApp fue interrumpido");
        } catch (Exception ex) {
            log.warn(
                    "No se pudo enviar WhatsApp phoneNumberId={} errorType={} detail={}",
                    config.getPhoneNumberId(),
                    ex.getClass().getSimpleName(),
                    safeDetail(ex)
            );
            throw new BusinessException("CRM_WHATSAPP_NO_DISPONIBLE", "No se pudo conectar con WhatsApp Cloud API");
        }
    }

    public void markAsRead(CrmCanalTokenConfig config, String metaMessageId) {
        String accessToken = secretEncryptionService.decrypt(config.getAccessToken());
        if (!hasText(accessToken) || !hasText(config.getPhoneNumberId()) || !hasText(metaMessageId)) {
            throw new BusinessException(
                    "CRM_WHATSAPP_CONFIG_INCOMPLETA",
                    "No se pudo preparar la confirmacion de lectura para Meta"
            );
        }

        String phoneNumberId = validatePathSegment(config.getPhoneNumberId(), "Phone number ID");
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("messaging_product", "whatsapp");
        payload.put("status", "read");
        payload.put("message_id", metaMessageId);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(graphBaseUrl + "/" + graphApiVersion + "/" + phoneNumberId + "/messages"))
                    .timeout(readTimeout)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                JsonNode responseJson = parseResponse(response.body());
                String metaMessage = responseJson.path("error").path("message").asText("Meta rechazo la confirmacion de lectura");
                throw new BusinessException("CRM_WHATSAPP_META_ERROR", metaMessage);
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException("CRM_WHATSAPP_LECTURA_INTERRUMPIDA", "La confirmacion de lectura fue interrumpida");
        } catch (Exception ex) {
            throw new BusinessException("CRM_WHATSAPP_NO_DISPONIBLE", "No se pudo confirmar la lectura en WhatsApp");
        }
    }

    public ConnectionCheck testConnection(CrmCanalTokenConfig config) {
        OffsetDateTime checkedAt = OffsetDateTime.now(ZoneOffset.UTC);
        String accessToken = secretEncryptionService.decrypt(config.getAccessToken());
        String appSecret = secretEncryptionService.decrypt(config.getAppSecret());
        if (!hasText(accessToken)
                || !hasText(appSecret)
                || !hasText(config.getAppId())
                || !hasText(config.getWabaId())
                || !hasText(config.getPhoneNumberId())) {
            return ConnectionCheck.failed("Completa Access token, App ID, App secret, WABA ID y Phone number ID", checkedAt);
        }

        try {
            String appId = validatePathSegment(config.getAppId(), "App ID");
            String wabaId = validatePathSegment(config.getWabaId(), "WABA ID");
            String phoneNumberId = validatePathSegment(config.getPhoneNumberId(), "Phone number ID");

            String encodedToken = URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
            HttpJsonResult debugResult = getJson(
                    graphBaseUrl + "/" + graphApiVersion + "/debug_token?input_token=" + encodedToken,
                    appId + "|" + appSecret
            );
            if (!debugResult.success()) {
                return ConnectionCheck.failed(metaError(debugResult.body(), "Meta rechazo el Access token o el App secret"), checkedAt);
            }

            JsonNode tokenData = debugResult.body().path("data");
            boolean tokenValid = tokenData.path("is_valid").asBoolean(false)
                    && appId.equals(tokenData.path("app_id").asText(appId));
            Set<String> permissions = extractPermissions(tokenData);
            boolean permissionsValid = permissions.contains("whatsapp_business_messaging")
                    && permissions.contains("whatsapp_business_management");
            OffsetDateTime tokenExpiresAt = epochSeconds(tokenData.path("expires_at").asLong(0));
            if (!tokenValid) {
                return new ConnectionCheck(false, false, null, null, null, tokenExpiresAt,
                        List.copyOf(permissions), "El Access token no es valido para esta App de Meta", checkedAt);
            }
            if (!permissionsValid) {
                return new ConnectionCheck(false, false, null, null, null, tokenExpiresAt,
                        List.copyOf(permissions), "El token no tiene whatsapp_business_messaging y whatsapp_business_management", checkedAt);
            }

            HttpJsonResult phonesResult = getJson(
                    graphBaseUrl + "/" + graphApiVersion + "/" + wabaId
                            + "/phone_numbers?fields=id,display_phone_number,verified_name,quality_rating",
                    accessToken
            );
            if (!phonesResult.success()) {
                return new ConnectionCheck(false, false, null, null, null, tokenExpiresAt,
                        List.copyOf(permissions), metaError(phonesResult.body(), "No se pudo consultar el WABA ID"), checkedAt);
            }

            JsonNode configuredPhone = null;
            for (JsonNode phone : phonesResult.body().path("data")) {
                if (phoneNumberId.equals(phone.path("id").asText())) {
                    configuredPhone = phone;
                    break;
                }
            }
            if (configuredPhone == null) {
                return new ConnectionCheck(false, false, null, null, null, tokenExpiresAt,
                        List.copyOf(permissions), "El Phone number ID no pertenece al WABA ID configurado", checkedAt);
            }

            HttpJsonResult subscriptionsResult = getJson(
                    graphBaseUrl + "/" + graphApiVersion + "/" + wabaId + "/subscribed_apps",
                    accessToken
            );
            boolean subscribed = subscriptionsResult.success() && isAppSubscribed(subscriptionsResult.body(), appId);
            String message = subscribed
                    ? "Credenciales validas y aplicacion suscrita al WABA"
                    : subscriptionsResult.success()
                            ? "Credenciales validas, pero la App no esta suscrita al WABA"
                            : metaError(subscriptionsResult.body(), "Credenciales validas, pero no se pudo comprobar la suscripcion al WABA");
            return new ConnectionCheck(
                    true,
                    subscribed,
                    configuredPhone.path("display_phone_number").asText(null),
                    configuredPhone.path("verified_name").asText(null),
                    configuredPhone.path("quality_rating").asText(null),
                    tokenExpiresAt,
                    List.copyOf(permissions),
                    message,
                    checkedAt
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return ConnectionCheck.failed("La comprobacion con Meta fue interrumpida", checkedAt);
        } catch (Exception ex) {
            return ConnectionCheck.failed("No se pudo conectar con Meta Graph API", checkedAt);
        }
    }

    private HttpJsonResult getJson(String url, String bearerToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(readTimeout)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return new HttpJsonResult(response.statusCode(), parseResponse(response.body()));
    }

    private Set<String> extractPermissions(JsonNode tokenData) {
        Set<String> permissions = new LinkedHashSet<>();
        tokenData.path("scopes").forEach((scope) -> permissions.add(scope.asText()));
        tokenData.path("granular_scopes").forEach((scope) -> {
            String name = scope.path("scope").asText(null);
            if (hasText(name)) {
                permissions.add(name);
            }
        });
        permissions.removeIf((value) -> !hasText(value));
        return permissions;
    }

    private boolean isAppSubscribed(JsonNode body, String appId) {
        for (JsonNode subscription : body.path("data")) {
            String subscribedAppId = subscription.path("whatsapp_business_api_data").path("id").asText(
                    subscription.path("id").asText(null)
            );
            if (appId.equals(subscribedAppId)) {
                return true;
            }
        }
        return false;
    }

    private String metaError(JsonNode body, String fallback) {
        JsonNode error = body.path("error");
        String message = error.path("message").asText(fallback);
        String code = error.path("code").asText("");
        String subcode = error.path("error_subcode").asText("");
        String reference = code.isBlank() ? "" : " [Meta " + code + (subcode.isBlank() ? "" : "/" + subcode) + "]";
        return message + reference;
    }

    private String safeDetail(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.replaceAll("(?i)(access[_ -]?token|secret|authorization)[=: ]+\\S+", "$1=***");
    }

    private OffsetDateTime epochSeconds(long value) {
        return value <= 0 ? null : OffsetDateTime.ofInstant(Instant.ofEpochSecond(value), ZoneOffset.UTC);
    }

    private JsonNode parseResponse(String body) {
        try {
            return objectMapper.readTree(body == null || body.isBlank() ? "{}" : body);
        } catch (Exception ex) {
            throw new BusinessException("CRM_WHATSAPP_RESPUESTA_INVALIDA", "Meta devolvio una respuesta no valida");
        }
    }

    private String validateVersion(String value) {
        String version = value == null ? "" : value.trim();
        if (!version.matches("v[0-9]+(\\.[0-9]+)?")) {
            throw new IllegalArgumentException("WHATSAPP_GRAPH_API_VERSION debe tener formato v25.0");
        }
        return version;
    }

    private String validatePathSegment(String value, String field) {
        String segment = value == null ? "" : value.trim();
        if (!segment.matches("[A-Za-z0-9_-]+")) {
            throw new BusinessException("CRM_WHATSAPP_CONFIG_INVALIDA", field + " no es valido");
        }
        return segment;
    }

    private String stripTrailingSlash(String value) {
        String base = value == null ? "" : value.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record SendResult(String metaMessageId, String whatsappId, String rawResponse) {
    }

    private record HttpJsonResult(int statusCode, JsonNode body) {
        private boolean success() {
            return statusCode >= 200 && statusCode < 300;
        }
    }

    public record ConnectionCheck(
            boolean metaAccessValid,
            boolean wabaSubscribed,
            String displayPhoneNumber,
            String verifiedName,
            String qualityRating,
            OffsetDateTime tokenExpiresAt,
            List<String> permissions,
            String message,
            OffsetDateTime checkedAt
    ) {
        public static ConnectionCheck failed(String message, OffsetDateTime checkedAt) {
            return new ConnectionCheck(false, false, null, null, null, null, List.of(), message, checkedAt);
        }
    }
}
