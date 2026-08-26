package com.azurion.saascore.cotizaciones.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.crm.domain.entities.CrmCurrencyConfig;
import com.azurion.saascore.crm.domain.repositories.CrmCurrencyConfigRepository;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommercialCurrencyServiceTest {

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private CrmCurrencyConfigRepository currencyConfigRepository;

    private CommercialCurrencyService service;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("tenant-demo");
        service = new CommercialCurrencyService(empresaRepository, currencyConfigRepository);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void usesTenantBaseCurrencyWithoutExchangeConfiguration() {
        Empresa empresa = companyWithCurrency("USD");
        when(empresaRepository.findByTenantId("tenant-demo")).thenReturn(Optional.of(empresa));

        CommercialCurrencyService.CurrencySnapshot snapshot = service.resolve(null, null);

        assertThat(snapshot.currency()).isEqualTo("USD");
        assertThat(snapshot.baseCurrency()).isEqualTo("USD");
        assertThat(snapshot.exchangeRate()).isEqualByComparingTo("1.000000");
        assertThat(service.toBase(new BigDecimal("125.55"), snapshot)).isEqualByComparingTo("125.55");
    }

    @Test
    void appliesConfiguredRateAndCommercialMargin() {
        Empresa empresa = companyWithCurrency("PEN");
        CrmCurrencyConfig usd = new CrmCurrencyConfig();
        usd.setMoneda("USD");
        usd.setActivo(true);
        usd.setTipoCambioBase(new BigDecimal("3.800000"));
        usd.setMargenConversionPorcentaje(new BigDecimal("2.5000"));
        when(empresaRepository.findByTenantId("tenant-demo")).thenReturn(Optional.of(empresa));
        when(currencyConfigRepository.findByMoneda("USD")).thenReturn(Optional.of(usd));

        CommercialCurrencyService.CurrencySnapshot snapshot = service.resolve("usd", null);

        assertThat(snapshot.exchangeRate()).isEqualByComparingTo("3.895000");
        assertThat(service.toBase(new BigDecimal("100.00"), snapshot)).isEqualByComparingTo("389.50");
    }

    @Test
    void rejectsInactiveAlternativeCurrency() {
        Empresa empresa = companyWithCurrency("PEN");
        CrmCurrencyConfig eur = new CrmCurrencyConfig();
        eur.setMoneda("EUR");
        eur.setActivo(false);
        when(empresaRepository.findByTenantId("tenant-demo")).thenReturn(Optional.of(empresa));
        when(currencyConfigRepository.findByMoneda("EUR")).thenReturn(Optional.of(eur));

        assertThatThrownBy(() -> service.resolve("EUR", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no está habilitada");
    }

    private Empresa companyWithCurrency(String currency) {
        Empresa empresa = new Empresa();
        empresa.setTenantId("tenant-demo");
        empresa.setMonedaCodigo(currency);
        return empresa;
    }
}
