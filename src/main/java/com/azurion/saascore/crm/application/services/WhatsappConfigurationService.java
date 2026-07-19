package com.azurion.saascore.crm.application.services;

import com.azurion.saascore.crm.application.dto.WhatsappConnectionStatusResponse;
import com.azurion.saascore.crm.application.dto.WhatsappVerifyTokenResponse;
import com.azurion.saascore.crm.domain.entities.CrmCanalTokenConfig;
import com.azurion.saascore.crm.domain.repositories.CrmCanalTokenConfigRepository;
import com.azurion.saascore.crm.infrastructure.http.WhatsappCloudApiClient;
import com.azurion.saascore.crm.infrastructure.http.WhatsappCloudApiClient.ConnectionCheck;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WhatsappConfigurationService {

    private static final String CHANNEL = "WHATSAPP";

    private final CrmCanalTokenConfigRepository configRepository;
    private final CrmSecretEncryptionService secretEncryptionService;
    private final WhatsappCloudApiClient cloudApiClient;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public WhatsappVerifyTokenResponse generateVerifyToken() {
        CrmCanalTokenConfig config = configRepository.findByCanal(CHANNEL).orElseGet(this::newConfig);
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String verifyToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        config.setVerifyToken(secretEncryptionService.encrypt(verifyToken));
        config.setWebhookVerifiedAt(null);
        configRepository.save(config);
        return new WhatsappVerifyTokenResponse(verifyToken, OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Transactional
    public WhatsappConnectionStatusResponse testConnection() {
        CrmCanalTokenConfig config = configRepository.findByCanal(CHANNEL).orElseGet(this::newConfig);
        ConnectionCheck check = cloudApiClient.testConnection(config);
        config.setLastConnectionTestAt(check.checkedAt());
        config.setLastConnectionOk(check.metaAccessValid());
        config.setLastConnectionMessage(truncate(check.message(), 500));
        config.setWabaSubscribed(check.wabaSubscribed());
        config.setMetaDisplayPhoneNumber(check.displayPhoneNumber());
        config.setMetaVerifiedName(check.verifiedName());
        config.setMetaQualityRating(check.qualityRating());
        config.setMetaTokenExpiresAt(check.tokenExpiresAt());
        configRepository.save(config);
        return toStatus(config, check.permissions());
    }

    @Transactional(readOnly = true)
    public WhatsappConnectionStatusResponse getStatus() {
        return configRepository.findByCanal(CHANNEL)
                .map((config) -> toStatus(config, List.of()))
                .orElseGet(() -> toStatus(newConfig(), List.of()));
    }

    private WhatsappConnectionStatusResponse toStatus(CrmCanalTokenConfig config, List<String> permissions) {
        boolean complete = isConfigurationComplete(config);
        boolean tokenExpired = config.getMetaTokenExpiresAt() != null
                && !config.getMetaTokenExpiresAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC));
        boolean metaAccessValid = Boolean.TRUE.equals(config.getLastConnectionOk()) && !tokenExpired;
        boolean subscribed = Boolean.TRUE.equals(config.getWabaSubscribed());
        boolean webhookVerified = config.getWebhookVerifiedAt() != null;
        boolean connected = config.isActivo() && complete && metaAccessValid && subscribed && webhookVerified;
        String message = config.getLastConnectionMessage();
        if (tokenExpired) {
            message = "El Access token de Meta esta vencido";
        } else if (message == null && !complete) {
            message = "La configuracion de WhatsApp esta incompleta";
        } else if (message == null && config.getLastConnectionTestAt() == null) {
            message = "La conexion con Meta aun no fue probada";
        }
        return new WhatsappConnectionStatusResponse(
                config.isActivo(),
                complete,
                metaAccessValid,
                subscribed,
                webhookVerified,
                connected,
                config.getMetaDisplayPhoneNumber(),
                config.getMetaVerifiedName(),
                config.getMetaQualityRating(),
                config.getMetaTokenExpiresAt(),
                permissions == null ? List.of() : List.copyOf(permissions),
                message,
                config.getLastConnectionTestAt(),
                config.getWebhookVerifiedAt()
        );
    }

    private boolean isConfigurationComplete(CrmCanalTokenConfig config) {
        return hasText(config.getAccessToken())
                && hasText(config.getVerifyToken())
                && hasText(config.getAppId())
                && hasText(config.getAppSecret())
                && hasText(config.getPhoneNumberId())
                && hasText(config.getWabaId());
    }

    private CrmCanalTokenConfig newConfig() {
        CrmCanalTokenConfig config = new CrmCanalTokenConfig();
        config.setCanal(CHANNEL);
        config.setNombre("WhatsApp Business");
        config.setActivo(false);
        return config;
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
