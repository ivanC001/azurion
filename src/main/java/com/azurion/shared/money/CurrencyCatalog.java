package com.azurion.shared.money;

import com.azurion.shared.exception.BusinessException;
import java.util.Currency;
import java.util.Locale;
import java.util.Map;

public final class CurrencyCatalog {

    private static final Locale SPANISH = Locale.forLanguageTag("es-PE");
    private static final Map<String, String> SYMBOLS = Map.ofEntries(
            Map.entry("PEN", "S/"),
            Map.entry("USD", "US$"),
            Map.entry("EUR", "€"),
            Map.entry("MXN", "MX$"),
            Map.entry("COP", "COL$"),
            Map.entry("CLP", "CLP$"),
            Map.entry("ARS", "AR$"),
            Map.entry("BRL", "R$"),
            Map.entry("CAD", "CA$"),
            Map.entry("GBP", "£"),
            Map.entry("JPY", "¥")
    );

    private CurrencyCatalog() {
    }

    public static String normalize(String value, String errorCode) {
        String code = value == null ? null : value.trim().toUpperCase(Locale.ROOT);
        if (code == null || code.isBlank()) {
            throw new BusinessException(errorCode, "Selecciona una moneda ISO válida");
        }
        try {
            Currency.getInstance(code);
            return code;
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(errorCode, "La moneda " + code + " no pertenece al catálogo ISO 4217");
        }
    }

    public static String displayName(String code) {
        return Currency.getInstance(code).getDisplayName(SPANISH);
    }

    public static String symbol(String code) {
        String normalized = code == null ? "PEN" : code.trim().toUpperCase(Locale.ROOT);
        return SYMBOLS.getOrDefault(normalized, normalized);
    }
}
