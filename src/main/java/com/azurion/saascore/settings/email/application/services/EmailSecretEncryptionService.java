package com.azurion.saascore.settings.email.application.services;

import com.azurion.shared.exception.BusinessException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
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
    private final SecretKeySpec secretKey;

    public EmailSecretEncryptionService(@Value("${azurion.security.email-secret-key}") String secret) {
        if (secret == null || secret.isBlank() || secret.length() < 16) {
            throw new BusinessException("EMAIL_SECRET_KEY_INVALID", "Configura AZURION_EMAIL_SECRET_KEY con una clave segura.");
        }
        this.secretKey = new SecretKeySpec(sha256(secret), "AES");
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
            byte[] iv = Arrays.copyOfRange(allBytes, 0, IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(allBytes, IV_LENGTH, allBytes.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new BusinessException("EMAIL_SECRET_DECRYPT_ERROR", "No se pudo descifrar la clave SMTP.");
        }
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new BusinessException("EMAIL_SECRET_KEY_ERROR", "No se pudo preparar la clave de cifrado SMTP.");
        }
    }
}
