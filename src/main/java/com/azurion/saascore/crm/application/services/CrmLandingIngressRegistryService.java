package com.azurion.saascore.crm.application.services;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.crm.domain.entities.CrmLandingConfig;
import com.azurion.saascore.crm.domain.entities.CrmLandingIngressRegistry;
import com.azurion.saascore.crm.domain.repositories.CrmLandingIngressRegistryRepository;
import com.azurion.shared.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CrmLandingIngressRegistryService {

    private static final SecureRandom SECRET_RANDOM = new SecureRandom();
    private static final String SIGNATURE_PREFIX = "sha256=";

    private final CrmLandingIngressRegistryRepository repository;
    private final CrmSecretEncryptionService secretEncryptionService;

    @Value("${azurion.crm.public-ingress.signature-tolerance-seconds:300}")
    private long signatureToleranceSeconds;

    @Transactional
    public LandingIngressCredentials synchronize(CrmLandingConfig landing) {
        return synchronize(landing, true);
    }

    @Transactional
    public LandingIngressCredentials synchronize(CrmLandingConfig landing, boolean revealRelaySecret) {
        String tenantId = requireCurrentTenant();
        CrmLandingIngressRegistry registry = repository
                .findByTenantIdAndLandingConfigId(tenantId, landing.getId())
                .orElseGet(CrmLandingIngressRegistry::new);
        registry.setTenantId(tenantId);
        registry.setLandingConfigId(landing.getId());
        registry.setSourceKey(landing.getLandingKey());
        registry.setActivo(landing.isActiva() && landing.isRecibirLeads());
        if (!hasText(registry.getRelaySecretEncrypted())) {
            registry.setRelaySecretEncrypted(secretEncryptionService.encrypt(generateSecret()));
        }
        CrmLandingIngressRegistry saved = repository.save(registry);
        return revealRelaySecret
                ? credentials(saved)
                : new LandingIngressCredentials(saved.getSourceKey(), null);
    }

    @Transactional
    public LandingIngressCredentials regenerateRelaySecret(CrmLandingConfig landing) {
        String tenantId = requireCurrentTenant();
        CrmLandingIngressRegistry registry = repository
                .findByTenantIdAndLandingConfigId(tenantId, landing.getId())
                .orElseGet(CrmLandingIngressRegistry::new);
        registry.setTenantId(tenantId);
        registry.setLandingConfigId(landing.getId());
        registry.setSourceKey(landing.getLandingKey());
        registry.setActivo(landing.isActiva() && landing.isRecibirLeads());
        registry.setRelaySecretEncrypted(secretEncryptionService.encrypt(generateSecret()));
        return credentials(repository.save(registry));
    }

    @Transactional(readOnly = true)
    public ResolvedIngress resolveBrowserSource(String sourceKey) {
        CrmLandingIngressRegistry registry = findActiveSource(sourceKey);
        return new ResolvedIngress(registry.getSourceKey(), registry.getTenantId(), registry.getLandingConfigId());
    }

    @Transactional(readOnly = true)
    public ResolvedIngress verifyRelay(String sourceKey,
                                       String timestamp,
                                       String idempotencyKey,
                                       String rawBody,
                                       String signature) {
        CrmLandingIngressRegistry registry = findActiveSource(sourceKey);
        long requestEpoch = parseTimestamp(timestamp);
        long difference = Math.abs(Instant.now().getEpochSecond() - requestEpoch);
        if (difference > Math.max(30L, signatureToleranceSeconds)) {
            throw invalidSignature();
        }
        if (!hasText(idempotencyKey) || idempotencyKey.trim().length() > 120) {
            throw invalidSignature();
        }

        String provided = trim(signature);
        if (provided == null || !provided.startsWith(SIGNATURE_PREFIX)) {
            throw invalidSignature();
        }
        String secret = secretEncryptionService.decrypt(registry.getRelaySecretEncrypted());
        String signedPayload = timestamp.trim() + "." + idempotencyKey.trim() + "." + rawBody;
        String expected = SIGNATURE_PREFIX + hmacSha256Hex(secret, signedPayload);
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                provided.getBytes(StandardCharsets.US_ASCII)
        )) {
            throw invalidSignature();
        }
        return new ResolvedIngress(registry.getSourceKey(), registry.getTenantId(), registry.getLandingConfigId());
    }

    private CrmLandingIngressRegistry findActiveSource(String sourceKey) {
        String normalized = trim(sourceKey);
        if (normalized == null || normalized.length() > 120) {
            throw invalidSource();
        }
        return repository.findBySourceKeyAndActivoTrue(normalized)
                .orElseThrow(this::invalidSource);
    }

    private LandingIngressCredentials credentials(CrmLandingIngressRegistry registry) {
        return new LandingIngressCredentials(
                registry.getSourceKey(),
                secretEncryptionService.decrypt(registry.getRelaySecretEncrypted())
        );
    }

    private long parseTimestamp(String timestamp) {
        try {
            return Long.parseLong(timestamp == null ? "" : timestamp.trim());
        } catch (NumberFormatException ex) {
            throw invalidSignature();
        }
    }

    private String hmacSha256Hex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return java.util.HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw BusinessException.internal("CRM_RELAY_SIGNATURE_ERROR", "No se pudo validar la firma del relay");
        }
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        SECRET_RANDOM.nextBytes(bytes);
        return "rls_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String requireCurrentTenant() {
        String tenantId = trim(TenantContext.getTenantId());
        if (tenantId == null || TenantContext.DEFAULT_TENANT.equalsIgnoreCase(tenantId)) {
            throw new BusinessException("CRM_TENANT_REQUERIDO", "No se pudo identificar el tenant de la landing");
        }
        return tenantId;
    }

    private BusinessException invalidSource() {
        return BusinessException.notFound("CRM_LANDING_SOURCE_INVALIDA", "La fuente de landing no existe o esta desactivada");
    }

    private BusinessException invalidSignature() {
        return BusinessException.forbidden("CRM_RELAY_FIRMA_INVALIDA", "La firma del relay no es valida");
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record LandingIngressCredentials(String sourceKey, String relaySecret) {
    }

    public record ResolvedIngress(String sourceKey, String tenantId, Long landingConfigId) {
    }
}
