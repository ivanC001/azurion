package com.azurion.saascore.crm.application.services;

import com.azurion.saascore.crm.domain.entities.CrmPublicLeadSubmission;
import com.azurion.saascore.crm.domain.repositories.CrmPublicLeadSubmissionRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicLeadSubmissionAuditService {

    private final CrmPublicLeadSubmissionRepository submissionRepository;
    private final CrmIngressLockService ingressLockService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRejected(String sourceKey,
                               String sourceType,
                               String idempotencyKey,
                               String errorCode,
                               String userMessage) {
        try {
            String idempotencyHash = hash(sourceKey, idempotencyKey);
            if (idempotencyHash != null) {
                ingressLockService.lockAll(List.of("crm-lead:idempotency:" + idempotencyHash));
                if (submissionRepository.findByIdempotencyHash(idempotencyHash).isPresent()) {
                    return;
                }
            }
            CrmPublicLeadSubmission submission = new CrmPublicLeadSubmission();
            submission.setReceiptId(generateReceipt());
            submission.setIdempotencyHash(idempotencyHash);
            submission.setSourceKey(trim(sourceKey));
            submission.setSourceType(normalizeSourceType(sourceType));
            submission.setEstado("REJECTED");
            submission.setErrorCode(trimTo(errorCode, 80));
            submission.setErrorMessage(trimTo(userMessage, 500));
            submission.setReceivedAt(OffsetDateTime.now());
            submission.setCompletedAt(OffsetDateTime.now());
            submissionRepository.save(submission);
        } catch (RuntimeException auditFailure) {
            log.error("No se pudo auditar el rechazo de un lead publico", auditFailure);
        }
    }

    private String hash(String sourceKey, String idempotencyKey) {
        String key = trim(idempotencyKey);
        if (key == null || key.length() > 120) {
            return null;
        }
        try {
            String material = (trim(sourceKey) == null ? "legacy" : sourceKey.trim()) + ":" + key;
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(material.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception ex) {
            return null;
        }
    }

    private String generateReceipt() {
        return "LD-" + UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 20)
                .toUpperCase(Locale.ROOT);
    }

    private String normalizeSourceType(String value) {
        String normalized = trim(value);
        return normalized == null ? "LEGACY" : normalized.toUpperCase(Locale.ROOT);
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String trimTo(String value, int maxLength) {
        String normalized = trim(value);
        return normalized == null || normalized.length() <= maxLength
                ? normalized
                : normalized.substring(0, maxLength);
    }
}
