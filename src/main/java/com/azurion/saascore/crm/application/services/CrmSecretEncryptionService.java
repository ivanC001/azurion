package com.azurion.saascore.crm.application.services;

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
public class CrmSecretEncryptionService {

    private static final String PREFIX = "crm:v1:";
    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;
    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKeySpec secretKey;

    public CrmSecretEncryptionService(@Value("${azurion.security.crm-secret-key}") String secret) {
        if (secret == null || secret.isBlank() || secret.length() < 16) {
            throw new BusinessException(
                    "CRM_SECRET_KEY_INVALIDA",
                    "Configura AZURION_CRM_SECRET_KEY con una clave segura de al menos 16 caracteres"
            );
        }
        this.secretKey = new SecretKeySpec(sha256(secret), "AES");
    }

    public String encrypt(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        if (rawValue.startsWith(PREFIX)) {
            return rawValue;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(rawValue.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return PREFIX + Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception ex) {
            throw BusinessException.internal("CRM_SECRET_ENCRYPT_ERROR", "No se pudo cifrar la credencial CRM");
        }
    }

    public String decrypt(String encryptedValue) {
        if (encryptedValue == null || encryptedValue.isBlank()) {
            return null;
        }
        // Compatibilidad con credenciales guardadas antes de esta migracion.
        if (!encryptedValue.startsWith(PREFIX)) {
            return encryptedValue;
        }
        try {
            byte[] allBytes = Base64.getDecoder().decode(encryptedValue.substring(PREFIX.length()));
            byte[] iv = Arrays.copyOfRange(allBytes, 0, IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(allBytes, IV_LENGTH, allBytes.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw BusinessException.internal("CRM_SECRET_DECRYPT_ERROR", "No se pudo descifrar la credencial CRM");
        }
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw BusinessException.internal("CRM_SECRET_KEY_ERROR", "No se pudo preparar la clave de cifrado CRM");
        }
    }
}
