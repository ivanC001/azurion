package com.azurion.saascore.cotizaciones.application.services;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.crm.domain.entities.CrmCurrencyConfig;
import com.azurion.saascore.crm.domain.repositories.CrmCurrencyConfigRepository;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.shared.exception.BusinessException;
import com.azurion.shared.money.CurrencyCatalog;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommercialCurrencyService {

    private final EmpresaRepository empresaRepository;
    private final CrmCurrencyConfigRepository currencyConfigRepository;

    public CurrencySnapshot resolve(String requestedCurrency, String fallbackCurrency) {
        String baseCurrency = CurrencyCatalog.normalize(resolveTenantBaseCurrency(), "MONEDA_BASE_INVALIDA");
        String requested = firstNonBlank(requestedCurrency, fallbackCurrency, baseCurrency);
        String currency = CurrencyCatalog.normalize(requested, "COTIZACION_MONEDA_INVALIDA");
        BigDecimal rate = currency.equals(baseCurrency) ? BigDecimal.ONE : configuredRate(currency);
        return new CurrencySnapshot(currency, baseCurrency, rate.setScale(6, RoundingMode.HALF_UP), OffsetDateTime.now());
    }

    public BigDecimal toBase(BigDecimal amount, CurrencySnapshot snapshot) {
        return money((amount == null ? BigDecimal.ZERO : amount).multiply(snapshot.exchangeRate()));
    }

    private BigDecimal configuredRate(String currency) {
        CrmCurrencyConfig config = currencyConfigRepository.findByMoneda(currency)
                .orElseThrow(() -> new BusinessException(
                        "CRM_MONEDA_NO_CONFIGURADA",
                        "Configura y activa el tipo de cambio de " + currency + " antes de crear la cotización"));
        if (!config.isActivo()) {
            throw new BusinessException(
                    "CRM_MONEDA_INACTIVA",
                    "La moneda " + currency + " no está habilitada para nuevas cotizaciones");
        }
        BigDecimal baseRate = config.getTipoCambioBase();
        if (baseRate == null || baseRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("CRM_TIPO_CAMBIO_INVALIDO", "Configura un tipo de cambio válido para " + currency);
        }
        BigDecimal margin = config.getMargenConversionPorcentaje() == null
                ? BigDecimal.ZERO
                : config.getMargenConversionPorcentaje();
        return baseRate.multiply(BigDecimal.ONE.add(
                margin.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP)));
    }

    private String resolveTenantBaseCurrency() {
        return empresaRepository.findByTenantId(TenantContext.getTenantId())
                .map(empresa -> empresa.getMonedaCodigo())
                .filter(value -> value != null && !value.isBlank())
                .orElse("PEN");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "PEN";
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public record CurrencySnapshot(
            String currency,
            String baseCurrency,
            BigDecimal exchangeRate,
            OffsetDateTime capturedAt
    ) {
    }
}
