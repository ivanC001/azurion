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
        String rawDigits = digits(rawValue);
        if (rawDigits == null) {
            return new NormalizedPhone(null, List.of());
        }
        String region = empresaRepository.findByTenantId(TenantContext.getTenantId())
                .map(empresa -> empresa.getPaisCodigo())
                .filter(value -> value != null && !value.isBlank())
                .map(String::toUpperCase)
                .orElse("ZZ");
        try {
            var parsed = PHONE_UTIL.parse(rawValue.trim(), region);
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

    private NormalizedPhone fallback(String rawDigits) {
        return new NormalizedPhone(rawDigits, List.of(rawDigits));
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
