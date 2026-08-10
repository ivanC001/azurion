package com.azurion.saascore.crm.application.services;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CrmPhoneNormalizationService {

    private static final PhoneNumberUtil PHONE_UTIL = PhoneNumberUtil.getInstance();

    private final EmpresaRepository empresaRepository;

    public NormalizedPhone normalize(String rawValue) {
        return normalize(rawValue, null);
    }

    /**
     * Normalizes a phone using the prospect's country whenever it is available.
     * Stored values are E.164 digits without the leading plus so they can be sent
     * directly to the WhatsApp Cloud API.
     */
    public NormalizedPhone normalize(String rawValue, String countryCode) {
        String rawDigits = digits(rawValue);
        if (rawDigits == null) {
            return new NormalizedPhone(null, List.of());
        }
        String region = resolveCountryCode(countryCode);
        try {
            var parsed = PHONE_UTIL.parse(parseInput(rawValue, rawDigits, region), region);
            if (!PHONE_UTIL.isPossibleNumber(parsed)) {
                return fallback(rawDigits);
            }
            String e164Digits = digits(PHONE_UTIL.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164));
            String nationalDigits = digits(PHONE_UTIL.getNationalSignificantNumber(parsed));
            LinkedHashSet<String> candidates = new LinkedHashSet<>();
            candidates.add(e164Digits);
            candidates.add(rawDigits);
            candidates.add(nationalDigits);
            candidates.remove(null);
            return new NormalizedPhone(e164Digits, List.copyOf(candidates));
        } catch (NumberParseException ignored) {
            return fallback(rawDigits);
        }
    }

    public String resolveCountryCode(String requestedCountryCode) {
        String requested = normalizeRegion(requestedCountryCode);
        if (requested != null) {
            return requested;
        }
        return empresaRepository.findByTenantId(TenantContext.getTenantId())
                .map(empresa -> normalizeRegion(empresa.getPaisCodigo()))
                .filter(value -> value != null)
                .orElse("ZZ");
    }

    public String countryCodeForPhone(String value) {
        String digits = digits(value);
        if (digits == null) {
            return null;
        }
        try {
            var parsed = PHONE_UTIL.parse("+" + digits, "ZZ");
            String region = PHONE_UTIL.getRegionCodeForNumber(parsed);
            return normalizeRegion(region);
        } catch (NumberParseException ignored) {
            return null;
        }
    }

    private NormalizedPhone fallback(String rawDigits) {
        return new NormalizedPhone(rawDigits, List.of(rawDigits));
    }

    private String parseInput(String rawValue, String rawDigits, String region) {
        String raw = rawValue.trim();
        if (raw.startsWith("+") || raw.startsWith("00") || "ZZ".equals(region)) {
            return raw;
        }
        int countryCallingCode = PHONE_UTIL.getCountryCodeForRegion(region);
        String callingCode = countryCallingCode <= 0 ? null : Integer.toString(countryCallingCode);
        if (callingCode != null && rawDigits.startsWith(callingCode)
                && rawDigits.length() > callingCode.length() + 5) {
            return "+" + rawDigits;
        }
        return raw;
    }

    private String normalizeRegion(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String region = value.trim().toUpperCase();
        return PHONE_UTIL.getSupportedRegions().contains(region) ? region : null;
    }

    private String digits(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.startsWith("00")) {
            digits = digits.substring(2);
        }
        return digits.isBlank() ? null : digits;
    }

    public record NormalizedPhone(String identity, List<String> lookupCandidates) {
    }
}
