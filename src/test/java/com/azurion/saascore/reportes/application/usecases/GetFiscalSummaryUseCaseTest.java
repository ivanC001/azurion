package com.azurion.saascore.reportes.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.azurion.saascore.inventory.domain.repositories.CompraRepository;
import com.azurion.saascore.facturacion.domain.repositories.NotaFiscalRepository;
import com.azurion.saascore.reportes.application.dto.FiscalSummaryResponse;
import com.azurion.saascore.ventas.domain.repositories.VentaDetalleRepository;
import com.azurion.saascore.ventas.domain.repositories.VentaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetFiscalSummaryUseCaseTest {

    @Mock VentaRepository ventaRepository;
    @Mock VentaDetalleRepository ventaDetalleRepository;
    @Mock CompraRepository compraRepository;
    @Mock NotaFiscalRepository notaFiscalRepository;

    private GetFiscalSummaryUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetFiscalSummaryUseCase(
                ventaRepository,
                ventaDetalleRepository,
                compraRepository,
                notaFiscalRepository
        );
        when(ventaRepository.sumGrossBetween(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(new BigDecimal("1180.00"));
        when(ventaDetalleRepository.sumNetSalesBetween(any(), any()))
                .thenReturn(new BigDecimal("1000.00"));
        when(ventaDetalleRepository.sumSalesTaxBetween(any(), any()))
                .thenReturn(new BigDecimal("180.00"));
        when(compraRepository.sumNetBetween(any(), any())).thenReturn(new BigDecimal("1200.00"));
        when(compraRepository.sumPurchaseTaxBetween(any(), any())).thenReturn(new BigDecimal("216.00"));
        when(compraRepository.sumTaxCreditBetween(any(), any())).thenReturn(new BigDecimal("216.00"));
        when(ventaDetalleRepository.sumKnownInventoryCostBetween(any(), any()))
                .thenReturn(new BigDecimal("650.00"));
        when(notaFiscalRepository.sumAcceptedGrossBetween(any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(notaFiscalRepository.sumAcceptedBaseBetween(any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(notaFiscalRepository.sumAcceptedTaxBetween(any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
    }

    @Test
    void separatesTaxPayableFromTaxCreditBalance() {
        when(ventaDetalleRepository.countMissingInventoryCostBetween(any(), any())).thenReturn(0L);

        FiscalSummaryResponse result = useCase.execute(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31)
        );

        assertThat(result.igvPorPagarEstimado()).isZero();
        assertThat(result.saldoCreditoFiscalEstimado()).isEqualByComparingTo("36.00");
        assertThat(result.margenReal()).isEqualByComparingTo("350.00");
        assertThat(result.margenCompleto()).isTrue();
    }

    @Test
    void doesNotPublishMisleadingMarginWhenHistoricalCostIsMissing() {
        when(ventaDetalleRepository.countMissingInventoryCostBetween(any(), any())).thenReturn(2L);

        FiscalSummaryResponse result = useCase.execute(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31)
        );

        assertThat(result.margenReal()).isNull();
        assertThat(result.margenCompleto()).isFalse();
        assertThat(result.lineasVentaSinCostoHistorico()).isEqualTo(2L);
    }
}
