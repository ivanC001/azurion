package com.azurion.saascore.inventory.application.usecases;

import com.azurion.saascore.inventory.application.dto.InventorySummaryResponse;
import com.azurion.saascore.inventory.domain.repositories.CompraDetalleRepository;
import com.azurion.saascore.inventory.domain.repositories.CompraRepository;
import com.azurion.saascore.inventory.domain.repositories.KardexMovimientoRepository;
import com.azurion.saascore.inventory.domain.repositories.StockLoteRepository;
import com.azurion.saascore.inventory.domain.repositories.StockRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetInventorySummaryUseCase {

    private final StockRepository stockRepository;
    private final StockLoteRepository stockLoteRepository;
    private final KardexMovimientoRepository kardexMovimientoRepository;
    private final CompraRepository compraRepository;
    private final CompraDetalleRepository compraDetalleRepository;

    @Transactional(readOnly = true)
    public InventorySummaryResponse execute() {
        LocalDate today = LocalDate.now();
        return new InventorySummaryResponse(
                stockRepository.count(),
                stockRepository.countLowStock(),
                stockRepository.countWithoutStock(),
                stockLoteRepository.countExpiring(today, today.plusDays(30)),
                stockLoteRepository.countExpired(today),
                kardexMovimientoRepository.count(),
                compraRepository.count(),
                compraRepository.sumRegisteredTotal(),
                compraDetalleRepository.sumProjectedProfit()
        );
    }
}
