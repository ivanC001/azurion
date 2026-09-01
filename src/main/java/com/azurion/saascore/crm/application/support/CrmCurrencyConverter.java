package com.azurion.saascore.crm.application.support;

import static com.azurion.saascore.crm.application.support.CrmSupport.firstNonBlank;
import static com.azurion.saascore.crm.application.support.CrmSupport.moneyOrZero;
import static com.azurion.saascore.crm.application.support.CrmSupport.trim;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.crm.domain.entities.CrmCurrencyConfig;
import com.azurion.saascore.crm.domain.repositories.CrmCurrencyConfigRepository;
import com.azurion.saascore.crm.domain.repositories.CrmOportunidadRepository;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Conversion de importes del CRM a la moneda base del tenant.
 *
 * Extraido de CrmUseCaseService: la conversion la usaban por igual el catalogo,
 * las oportunidades y los reportes, asi que era una dependencia compartida
 * escondida dentro de un servicio de casos de uso.
 */
@Component
@RequiredArgsConstructor
public class CrmCurrencyConverter {

    private static final String DEFAULT_BASE_CURRENCY = "PEN";

    private static final int RATE_SCALE = 8;

    private static final int AMOUNT_SCALE = 2;

    private final CrmCurrencyConfigRepository currencyConfigRepository;
    private final EmpresaRepository empresaRepository;

    public String currentTenantBaseCurrency() {
        return empresaRepository.findByTenantId(TenantContext.getTenantId())
                .map(empresa -> trim(empresa.getMonedaCodigo()))
                .filter(value -> value != null)
                .orElse(DEFAULT_BASE_CURRENCY);
    }

    public String normalizeCurrency(String value) {
        String normalized = firstNonBlank(value, currentTenantBaseCurrency()).toUpperCase(Locale.ROOT);
        try {
            Currency.getInstance(normalized);
            return normalized;
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("CRM_MONEDA_INVALIDA", "Selecciona una moneda ISO valida");
        }
    }

    public String requireEnabledQuoteCurrency(String value) {
        return requireEnabledCommercialCurrency(value, "cotización");
    }

    /**
     * Solo se admiten monedas con tipo de cambio configurado y activo: emitir
     * en una moneda sin tasa dejaria importes imposibles de consolidar.
     */
    public String requireEnabledCommercialCurrency(String value, String operation) {
        String currency = normalizeCurrency(value);
        String baseCurrency = currentTenantBaseCurrency().toUpperCase(Locale.ROOT);
        if (currency.equals(baseCurrency)) {
            return currency;
        }
        CrmCurrencyConfig config = currencyConfigRepository.findByMoneda(currency)
                .orElseThrow(() -> new BusinessException(
                        "CRM_MONEDA_NO_CONFIGURADA",
                        "Configura el tipo de cambio de " + currency + " antes de continuar con " + operation));
        if (!config.isActivo()) {
            throw new BusinessException(
                    "CRM_MONEDA_INACTIVA",
                    "La moneda " + currency + " no está disponible para nuevos productos ni cotizaciones");
        }
        if (config.getTipoCambioBase() == null || config.getTipoCambioBase().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(
                    "CRM_TIPO_CAMBIO_INVALIDO",
                    "Configura un tipo de cambio válido para " + currency);
        }
        return currency;
    }

    public BigDecimal sumCurrencyAmounts(List<CrmOportunidadRepository.CurrencyAmountProjection> rows) {
        return rows.stream()
                .map(row -> toTenantBase(row.getMonto(), row.getMoneda()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Convierte entre monedas comerciales pasando por la moneda base. Igual que
     * toTenantBase, una moneda destino sin tasa activa devuelve cero antes que
     * falsear el importe.
     */
    public BigDecimal convert(BigDecimal amount, String sourceCurrency, String targetCurrency) {
        BigDecimal base = toTenantBase(amount, sourceCurrency);
        String baseCurrency = currentTenantBaseCurrency().toUpperCase(Locale.ROOT);
        String target = trim(targetCurrency);
        if (target == null || baseCurrency.equalsIgnoreCase(target)) {
            return base;
        }
        BigDecimal rate = activeRate(target);
        if (rate == null) {
            return BigDecimal.ZERO.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
        }
        return base.divide(rate, AMOUNT_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Un importe en moneda sin tasa activa aporta cero al consolidado, en lugar
     * de sumarse como si fuese moneda base y falsear el total.
     */
    public BigDecimal toTenantBase(BigDecimal amount, String sourceCurrency) {
        BigDecimal normalizedAmount = moneyOrZero(amount);
        String baseCurrency = currentTenantBaseCurrency().toUpperCase(Locale.ROOT);
        String currency = trim(sourceCurrency);
        if (currency == null || baseCurrency.equalsIgnoreCase(currency)) {
            return normalizedAmount;
        }
        BigDecimal rate = activeRate(currency);
        if (rate == null) {
            return BigDecimal.ZERO.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
        }
        return normalizedAmount.multiply(rate).setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal activeRate(String currency) {
        CrmCurrencyConfig config = currencyConfigRepository.findByMoneda(currency.toUpperCase(Locale.ROOT))
                .orElse(null);
        if (config == null || !config.isActivo()
                || config.getTipoCambioBase() == null
                || config.getTipoCambioBase().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal margin = config.getMargenConversionPorcentaje() == null
                ? BigDecimal.ZERO
                : config.getMargenConversionPorcentaje();
        return config.getTipoCambioBase().multiply(
                BigDecimal.ONE.add(margin.divide(BigDecimal.valueOf(100), RATE_SCALE, RoundingMode.HALF_UP))
        );
    }
}
