package com.azurion.saascore.settings.email.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.azurion.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

class EmailSecretEncryptionServiceTest {

    private static final String CURRENT_KEY = "current-email-key-with-at-least-thirty-two-bytes";
    private static final String PREVIOUS_KEY = "previous-email-key-with-at-least-thirty-two-bytes";

    @Test
    void encryptsAndDecryptsWithoutPersistingPlaintext() {
        EmailSecretEncryptionService service = new EmailSecretEncryptionService(CURRENT_KEY, "");

        String encrypted = service.encrypt("smtp-password");

        assertThat(encrypted).startsWith("v1:").doesNotContain("smtp-password");
        assertThat(service.decrypt(encrypted)).isEqualTo("smtp-password");
    }

    @Test
    void decryptsWithPreviousKeyDuringRotation() {
        EmailSecretEncryptionService oldService = new EmailSecretEncryptionService(PREVIOUS_KEY, "");
        String encrypted = oldService.encrypt("smtp-password");
        EmailSecretEncryptionService rotated = new EmailSecretEncryptionService(CURRENT_KEY, PREVIOUS_KEY);

        assertThat(rotated.decrypt(encrypted)).isEqualTo("smtp-password");
    }

    @Test
    void rejectsWeakKeysAndTamperedPayloads() {
        assertThatThrownBy(() -> new EmailSecretEncryptionService("change-me", ""))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getCode())
                .isEqualTo("EMAIL_SECRET_KEY_INVALID");

        EmailSecretEncryptionService service = new EmailSecretEncryptionService(CURRENT_KEY, "");
        assertThatThrownBy(() -> service.decrypt("v1:not-valid-base64"))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getCode())
                .isEqualTo("EMAIL_SECRET_DECRYPT_ERROR");
    }
}
