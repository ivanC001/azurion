package com.azurion.saascore.crm.application.support;

import com.azurion.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Utilidades transversales del modulo CRM: normalizacion de texto, validacion
 * de enumerados y paginacion segura.
 *
 * Vivian como metodos privados dentro de CrmUseCaseService. Al extraerlas aqui
 * pueden compartirlas los casos de uso que se van separando de ese servicio,
 * sin duplicar reglas de validacion.
 */
public final class CrmSupport {

    private static final int MAX_PAGE_SIZE = 100;

    private CrmSupport() {
    }

    public static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public static String firstNonBlank(String... values) {
        for (String value : values) {
            String trimmed = trim(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    public static String required(String value, String message) {
        String normalized = trim(value);
        if (normalized == null) {
            throw new BusinessException("CRM_DATO_REQUERIDO", message);
        }
        return normalized;
    }

    public static String normalize(String value) {
        return required(value, "Campo CRM obligatorio").toUpperCase(Locale.ROOT);
    }

    public static String requireEnum(String value, Set<String> allowed, String code) {
        String normalized = normalize(value);
        if (!allowed.contains(normalized)) {
            throw new BusinessException(code, "Valor CRM invalido: " + value);
        }
        return normalized;
    }

    public static String optionalEnum(String value, Set<String> allowed, String code) {
        String trimmed = trim(value);
        return trimmed == null ? null : requireEnum(trimmed, allowed, code);
    }

    public static String defaultEnum(String value, String defaultValue, Set<String> allowed, String code) {
        return value == null || value.isBlank() ? defaultValue : requireEnum(value, allowed, code);
    }

    public static String normalizeSearch(String value) {
        String normalized = trim(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    public static String likePattern(String query) {
        return "%" + query.toLowerCase(Locale.ROOT) + "%";
    }

    /**
     * Acota pagina y tamano para que un cliente no pueda pedir toda la tabla.
     */
    public static Pageable safePageable(int page, int size, Sort sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize, sort);
    }

    public static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Sustituye null por cero sin reescalar: se usa en agregados donde la
     * escala la fija el consumidor.
     */
    public static BigDecimal moneyOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public static BigDecimal nonNegative(BigDecimal value) {
        BigDecimal resolved = value == null ? BigDecimal.ZERO : value;
        if (resolved.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("CRM_MONTO_INVALIDO", "El monto no puede ser negativo");
        }
        return resolved;
    }

    public static <T> void updateIfPresent(T value, Consumer<T> consumer) {
        if (value != null) {
            consumer.accept(value);
        }
    }
}
