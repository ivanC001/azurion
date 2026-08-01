package com.azurion.saascore.inventory.application.services;

import com.azurion.shared.exception.BusinessException;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ProductoPhotoValidator {

    public static final int MAX_PHOTO_BYTES = 1024 * 1024;
    private static final int MAX_DATA_URL_LENGTH = 1_400_000;
    private static final Pattern DATA_IMAGE_PATTERN = Pattern.compile(
            "^data:(image/(?:png|jpeg|jpg|webp));base64,([a-zA-Z0-9+/=]+)$",
            Pattern.CASE_INSENSITIVE
    );

    private ProductoPhotoValidator() {
    }

    public static String validateNewPhoto(String value) {
        String photo = trim(value);
        if (photo == null) {
            return null;
        }
        if (!photo.regionMatches(true, 0, "data:", 0, 5)) {
            throw new BusinessException(
                    "PRODUCTO_FOTO_URL_NO_PERMITIDA",
                    "Selecciona una foto desde tu dispositivo; no se permiten URL externas."
            );
        }
        if (photo.length() > MAX_DATA_URL_LENGTH) {
            throw tooLarge();
        }

        Matcher matcher = DATA_IMAGE_PATTERN.matcher(photo);
        if (!matcher.matches()) {
            throw invalidPhoto();
        }

        String mimeType = matcher.group(1).toLowerCase(Locale.ROOT);
        byte[] content;
        try {
            content = Base64.getDecoder().decode(matcher.group(2));
        } catch (IllegalArgumentException exception) {
            throw invalidPhoto();
        }

        if (content.length > MAX_PHOTO_BYTES) {
            throw tooLarge();
        }
        if (!matchesSignature(mimeType, content)) {
            throw invalidPhoto();
        }

        String normalizedMimeType = "image/jpg".equals(mimeType) ? "image/jpeg" : mimeType;
        return "data:" + normalizedMimeType + ";base64," + Base64.getEncoder().encodeToString(content);
    }

    public static String preserveOrValidate(String requestedValue, String currentValue) {
        String requested = trim(requestedValue);
        String current = trim(currentValue);
        if (requested == null || requested.equals(current)) {
            return current;
        }
        return validateNewPhoto(requested);
    }

    private static boolean matchesSignature(String mimeType, byte[] content) {
        return switch (mimeType) {
            case "image/png" -> content.length >= 8
                    && unsigned(content[0]) == 0x89
                    && content[1] == 0x50
                    && content[2] == 0x4E
                    && content[3] == 0x47
                    && content[4] == 0x0D
                    && content[5] == 0x0A
                    && content[6] == 0x1A
                    && content[7] == 0x0A;
            case "image/jpeg", "image/jpg" -> content.length >= 3
                    && unsigned(content[0]) == 0xFF
                    && unsigned(content[1]) == 0xD8
                    && unsigned(content[2]) == 0xFF;
            case "image/webp" -> content.length >= 12
                    && content[0] == 'R'
                    && content[1] == 'I'
                    && content[2] == 'F'
                    && content[3] == 'F'
                    && content[8] == 'W'
                    && content[9] == 'E'
                    && content[10] == 'B'
                    && content[11] == 'P';
            default -> false;
        };
    }

    private static int unsigned(byte value) {
        return value & 0xFF;
    }

    private static BusinessException invalidPhoto() {
        return new BusinessException(
                "PRODUCTO_FOTO_INVALIDA",
                "Selecciona una imagen PNG, JPG o WEBP valida."
        );
    }

    private static BusinessException tooLarge() {
        return new BusinessException(
                "PRODUCTO_FOTO_DEMASIADO_GRANDE",
                "La foto del producto no debe superar 1 MB."
        );
    }

    private static String trim(String value) {
        return value == null || value.trim().isBlank() ? null : value.trim();
    }
}
