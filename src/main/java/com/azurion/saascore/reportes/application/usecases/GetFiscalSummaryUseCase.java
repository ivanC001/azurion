package com.azurion.saascore.reportes.application.usecases;

import com.azurion.saascore.inventory.domain.repositories.CompraRepository;
import com.azurion.saascore.facturacion.domain.entities.NotaFiscal;
import com.azurion.saascore.facturacion.domain.repositories.NotaFiscalRepository;
import com.azurion.saascore.reportes.application.dto.FiscalSummaryResponse;
import com.azurion.saascore.ventas.domain.repositories.VentaDetalleRepository;
import com.azurion.saascore.ventas.domain.repositories.VentaRepository;
import com.azurion.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetFiscalSummaryUseCase {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final LocalDate EARLIEST_SUPPORTED_DATE = LocalDate.of(2000, 1, 1);

    private final VentaRepository ventaRepository;
    private final VentaDetalleRepository ventaDetalleRepository;
    private final CompraRepository compraRepository;
    private final NotaFiscalRepository notaFiscalRepository;

    @Transactional(readOnly = true)
    public FiscalSummaryResponse execute(LocalDate requestedFrom, LocalDate requestedTo) {
        LocalDate today = LocalDate.now(LIMA);
        LocalDate from = requestedFrom == null ? EARLIEST_SUPPORTED_DATE : requestedFrom;
        LocalDate to = requestedTo == null ? today : requestedTo;
        if (from.isAfter(to)) {
            throw new BusinessException(
                    "RANGO_REPORTE_INVALIDO",
                    "La fecha inicial del reporte no puede ser posterior a la fecha final"
            );
        }

        OffsetDateTime fromTimestamp = from.atStartOfDay(LIMA).toOffsetDateTime();
        OffsetDateTime toExclusive = to.plusDays(1).atStartOfDay(LIMA).toOffsetDateTime();

        BigDecimal creditNoteGross = money(notaFiscalRepository.sumAcceptedGrossBetween(
                NotaFiscal.TIPO_DOCUMENTO_CREDITO, from, to));
        BigDecimal creditNoteBase = money(notaFiscalRepository.sumAcceptedBaseBetween(
                NotaFiscal.TIPO_DOCUMENTO_CREDITO, from, to));
        BigDecimal creditNoteTax = money(notaFiscalRepository.sumAcceptedTaxBetween(
                NotaFiscal.TIPO_DOCUMENTO_CREDITO, from, to));
        BigDecimal debitNoteGross = money(notaFiscalRepository.sumAcceptedGrossBetween(
                NotaFiscal.TIPO_DOCUMENTO_DEBITO, from, to));
        BigDecimal debitNoteBase = money(notaFiscalRepository.sumAcceptedBaseBetween(
                NotaFiscal.TIPO_DOCUMENTO_DEBITO, from, to));
        BigDecimal debitNoteTax = money(notaFiscalRepository.sumAcceptedTaxBetween(
                NotaFiscal.TIPO_DOCUMENTO_DEBITO, from, to));

        BigDecimal grossSales = money(ventaRepository.sumGrossBetween(fromTimestamp, toExclusive))
                .subtract(creditNoteGross).add(debitNoteGross);
        BigDecimal netSales = money(ventaDetalleRepository.sumNetSalesBetween(fromTimestamp, toExclusive))
                .subtract(creditNoteBase).add(debitNoteBase);
        BigDecimal salesTax = money(ventaDetalleRepository.sumSalesTaxBetween(fromTimestamp, toExclusive))
                .subtract(creditNoteTax).add(debitNoteTax);
        BigDecimal purchaseNet = money(compraRepository.sumNetBetween(from, to));
        BigDecimal purchaseTax = money(compraRepository.sumPurchaseTaxBetween(from, to));
        BigDecimal taxCredit = money(compraRepository.sumTaxCreditBetween(from, to));
        BigDecimal knownCost = money(
                ventaDetalleRepository.sumKnownInventoryCostBetween(fromTimestamp, toExclusive)
        );
        long missingCostLines = ventaDetalleRepository.countMissingInventoryCostBetween(
                fromTimestamp,
                toExclusive
        );
        long historicalNotes = notaFiscalRepository.countAcceptedMissingTaxBreakdown(from, to);
        long creditNotesWithoutCostReversal = notaFiscalRepository.countAcceptedCreditNotesBetween(from, to);
        boolean completeMargin = missingCostLines == 0
                && historicalNotes == 0
                && creditNotesWithoutCostReversal == 0;
        BigDecimal realMargin = completeMargin ? money(netSales.subtract(knownCost)) : null;
        BigDecimal taxBalance = salesTax.subtract(taxCredit);

        return new FiscalSummaryResponse(
                from,
                to,
                grossSales,
                netSales,
                salesTax,
                purchaseNet,
                purchaseTax,
                taxCredit,
                money(taxBalance.max(BigDecimal.ZERO)),
                money(taxBalance.negate().max(BigDecimal.ZERO)),
                knownCost,
                realMargin,
                completeMargin,
                missingCostLines,
                historicalNotes,
                creditNotesWithoutCostReversal
        );
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
