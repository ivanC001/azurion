package com.azurion.saascore.crm.application.services;

import com.azurion.saascore.crm.application.dto.MetaWebhookResult;
import com.azurion.saascore.crm.domain.entities.CrmCanalTokenConfig;
import com.azurion.saascore.crm.domain.repositories.CrmCanalTokenConfigRepository;
import com.azurion.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MetaWebhookService {

    private static final Set<String> SUPPORTED_CHANNELS = Set.of("FACEBOOK", "INSTAGRAM");

    private final CrmCanalTokenConfigRepository configRepository;
    private final CrmSecretEncryptionService secretEncryptionService;
    private final ObjectMapper objectMapper;

    @Transactional
    public String verify(String channel, String mode, String verifyToken, String challenge) {
        CrmCanalTokenConfig config = requireActiveConfig(channel);
        String expectedToken = secretEncryptionService.decrypt(config.getVerifyToken());
        if (!"subscribe".equals(mode) || !secureEquals(expectedToken, verifyToken) || !hasText(challenge)) {
            throw new BusinessException("CRM_META_WEBHOOK_VERIFICACION_INVALIDA", "Meta no pudo verificar el webhook de " + config.getNombre());
        }
        config.setWebhookVerifiedAt(OffsetDateTime.now(ZoneOffset.UTC));
        configRepository.save(config);
        return challenge;
    }

    @Transactional
    public MetaWebhookResult receive(String channel, String rawBody, String signature) {
        CrmCanalTokenConfig config = requireActiveConfig(channel);
        verifySignature(config, rawBody, signature);
        JsonNode root = parseJson(rawBody);
        String object = root.path("object").asText("");
        if (!("page".equals(object) || "instagram".equals(object))) {
            throw new BusinessException("CRM_META_WEBHOOK_OBJETO_INVALIDO", "El evento recibido no corresponde a Facebook o Instagram");
        }
        int events = root.path("entry").isArray() ? root.path("entry").size() : 0;
        config.setLastWebhookAt(OffsetDateTime.now(ZoneOffset.UTC));
        configRepository.save(config);
        return new MetaWebhookResult(config.getCanal(), events);
    }

    private CrmCanalTokenConfig requireActiveConfig(String channel) {
        String normalized = channel == null ? "" : channel.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_CHANNELS.contains(normalized)) {
            throw new BusinessException("CRM_META_CANAL_INVALIDO", "El webhook solo admite Facebook o Instagram");
        }
        CrmCanalTokenConfig config = configRepository.findByCanal(normalized)
                .orElseThrow(() -> new BusinessException("CRM_META_NO_CONFIGURADO", "El canal de Meta no esta configurado"));
        if (!config.isActivo()) {
            throw new BusinessException("CRM_META_INACTIVO", "La integracion " + config.getNombre() + " esta inactiva");
        }
        return config;
    }

    private JsonNode parseJson(String rawBody) {
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            if (root == null || !root.isObject()) {
                throw new BusinessException("CRM_META_PAYLOAD_INVALIDO", "El webhook de Meta no contiene un objeto JSON");
            }
            return root;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("CRM_META_PAYLOAD_INVALIDO", "El webhook de Meta no contiene JSON valido");
        }
    }

    private void verifySignature(CrmCanalTokenConfig config, String rawBody, String signature) {
        String appSecret = secretEncryptionService.decrypt(config.getAppSecret());
        if (!hasText(appSecret) || !hasText(signature) || !signature.startsWith("sha256=")) {
            throw new BusinessException("CRM_META_FIRMA_REQUERIDA", "Falta la firma X-Hub-Signature-256 de Meta");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = "sha256=" + HexFormat.of().formatHex(mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8)));
            if (!MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.US_ASCII),
                    signature.trim().getBytes(StandardCharsets.US_ASCII))) {
                throw new BusinessException("CRM_META_FIRMA_INVALIDA", "La firma del webhook de Meta no es valida");
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("CRM_META_FIRMA_ERROR", "No se pudo validar la firma del webhook de Meta");
        }
    }

    private boolean secureEquals(String left, String right) {
        if (!hasText(left) || !hasText(right)) {
            return false;
        }
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
