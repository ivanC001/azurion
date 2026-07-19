package com.azurion.shared.exception;

import java.util.Locale;
import java.util.Set;

final class ErrorExposurePolicy {

    private static final Set<String> INTERNAL_CODES = Set.of(
            "FACTURACION_PAYLOAD_INVALIDO",
            "CRM_WHATSAPP_META_ERROR",
            "CRM_WHATSAPP_RESPUESTA_INVALIDA",
            "CRM_WHATSAPP_ENVIO_INTERRUMPIDO",
            "CRM_WHATSAPP_LECTURA_INTERRUMPIDA",
            "CRM_WHATSAPP_NO_DISPONIBLE",
            "CRM_WHATSAPP_FIRMA_ERROR"
    );

    private static final Set<String> INTERNAL_PREFIXES = Set.of(
            "FACTURADOR_",
            "EMAIL_SECRET_",
            "CRM_SECRET_"
    );

    private static final Set<String> INTERNAL_SUFFIXES = Set.of(
            "_SAVE_ERROR",
            "_READ_ERROR",
            "_WRITE_ERROR",
            "_STORAGE_ERROR",
            "_ENCRYPT_ERROR",
            "_DECRYPT_ERROR",
            "_KEY_ERROR",
            "_HASH_ERROR",
            "_SIGN_ERROR",
            "_SCHEMA_ERROR",
            "_PDF_ERROR",
            "_SEND_ERROR",
            "_ERROR",
            "_PATH_INVALID",
            "_PATH_INVALIDO"
    );

    private ErrorExposurePolicy() {
    }

    static boolean isUserActionable(BusinessException exception) {
        if (!exception.isUserActionable()) {
            return false;
        }

        String code = exception.getCode();
        if (code == null || code.isBlank()) {
            return false;
        }

        String normalizedCode = code.toUpperCase(Locale.ROOT);
        if (INTERNAL_CODES.contains(normalizedCode)) {
            return false;
        }

        if (INTERNAL_PREFIXES.stream().anyMatch(normalizedCode::startsWith)) {
            return false;
        }

        return INTERNAL_SUFFIXES.stream().noneMatch(normalizedCode::endsWith);
    }
}
