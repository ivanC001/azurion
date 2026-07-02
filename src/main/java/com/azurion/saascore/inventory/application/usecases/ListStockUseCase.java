package com.azurion.saascore.inventory.application.usecases;

import com.azurion.saascore.inventory.application.dto.StockResponse;
import com.azurion.saascore.inventory.domain.repositories.StockRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListStockUseCase {

    private final StockRepository stockRepository;
    private final StockMovimientoUseCase stockMovimientoUseCase;

    public List<StockResponse> execute(Long productoId, Long almacenId) {
        if (productoId != null && almacenId != null) {
            return stockRepository.findByProductoIdAndAlmacenId(productoId, almacenId)
                    .stream()
                    .map(stockMovimientoUseCase::toStockResponse)
                    .toList();
        }

        if (productoId != null) {
            return stockRepository.findByProductoId(productoId).stream()
                    .map(stockMovimientoUseCase::toStockResponse)
                    .toList();
        }

        if (almacenId != null) {
            return stockRepository.findByAlmacenId(almacenId).stream()
                    .map(stockMovimientoUseCase::toStockResponse)
                    .toList();
        }

        return stockRepository.findAll().stream()
                .map(stockMovimientoUseCase::toStockResponse)
                .toList();
    }

    public List<StockResponse> executeBySucursal(Long sucursalId) {
        return stockRepository.findByAlmacenSucursalId(sucursalId).stream()
                .map(stockMovimientoUseCase::toStockResponse)
                .toList();
    }
}
