package com.azurion.saascore.inventory.application.usecases;

import com.azurion.saascore.inventory.application.dto.ProductSummaryResponse;
import com.azurion.saascore.inventory.domain.repositories.ProductoRepository;
import com.azurion.saascore.inventory.domain.repositories.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetProductSummaryUseCase {

    private final ProductoRepository productoRepository;
    private final StockRepository stockRepository;

    @Transactional(readOnly = true)
    public ProductSummaryResponse execute() {
        return new ProductSummaryResponse(
                productoRepository.count(),
                productoRepository.countByActivoTrue(),
                productoRepository.countByTipoProductoIgnoreCase("PRODUCTO"),
                productoRepository.countByTipoProductoIgnoreCase("SERVICIO"),
                stockRepository.countLowStock() + stockRepository.countWithoutStock()
        );
    }
}
