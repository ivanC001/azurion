package com.azurion.saascore.settings.email.application.services;

import com.azurion.shared.exception.BusinessException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailSecretEncryptionService {

    private static final String PREFIX = "v1:";
    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;
    private final SecureRandom secureRandom = new SecureRandom();
    private static final Set<String> FORBIDDEN_KEYS = Set.of(
            "dev-email-secret-change-me-32-bytes-minimum",
            "replace_this_with_very_long_secure_secret_32_chars_minimum",
            "change-me"
    );
    private final SecretKeySpec secretKey;
    private final List<SecretKeySpec> decryptionKeys;

    public EmailSecretEncryptionService(
            @Value("${azurion.security.email-secret-key}") String secret,
            @Value("${azurion.security.email-secret-key-previous:}") String previousSecret
    ) {
        this.secretKey = deriveKey(secret, true);
        List<SecretKeySpec> keys = new ArrayList<>();
        keys.add(secretKey);
        if (previousSecret != null && !previousSecret.isBlank() && !previousSecret.equals(secret)) {
            keys.add(deriveKey(previousSecret, false));
        }
        this.decryptionKeys = List.copyOf(keys);
    }

    public String encrypt(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(rawPassword.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return PREFIX + Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception ex) {
            throw new BusinessException("EMAIL_SECRET_ENCRYPT_ERROR", "No se pudo cifrar la clave SMTP.");
        }
    }

    public String decrypt(String encryptedPassword) {
        if (encryptedPassword == null || encryptedPassword.isBlank()) {
            return null;
        }
        try {
            String payload = encryptedPassword.startsWith(PREFIX)
                    ? encryptedPassword.substring(PREFIX.length())
                    : encryptedPassword;
            byte[] allBytes = Base64.getDecoder().decode(payload);
            if (allBytes.length <= IV_LENGTH) {
                throw new IllegalArgumentException("Payload cifrado incompleto");
            }
            byte[] iv = Arrays.copyOfRange(allBytes, 0, IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(allBytes, IV_LENGTH, allBytes.length);
            for (SecretKeySpec candidate : decryptionKeys) {
                try {
                    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                    cipher.init(Cipher.DECRYPT_MODE, candidate, new GCMParameterSpec(GCM_TAG_BITS, iv));
                    return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
                } catch (Exception ignored) {
                    // Try the explicitly configured previous key during a controlled rotation.
                }
            }
            throw new IllegalArgumentException("Ninguna clave configurada pudo descifrar el valor");
        } catch (Exception ex) {
            throw new BusinessException("EMAIL_SECRET_DECRYPT_ERROR", "No se pudo descifrar la clave SMTP.");
        }
    }

    private SecretKeySpec deriveKey(String value, boolean current) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.getBytes(StandardCharsets.UTF_8).length < 32
                || FORBIDDEN_KEYS.contains(normalized.toLowerCase())) {
            String variable = current ? "AZURION_EMAIL_SECRET_KEY" : "AZURION_EMAIL_SECRET_KEY_PREVIOUS";
            throw new BusinessException(
                    "EMAIL_SECRET_KEY_INVALID",
                    "Configura " + variable + " con una clave aleatoria de al menos 32 bytes."
            );
        }
        return new SecretKeySpec(sha256(normalized), "AES");
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new BusinessException("EMAIL_SECRET_KEY_ERROR", "No se pudo preparar la clave de cifrado SMTP.");
        }
    }
}
