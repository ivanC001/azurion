package com.azurion.saascore.inventory.application.usecases;

import com.azurion.saascore.inventory.application.dto.StockResponse;
import com.azurion.saascore.inventory.domain.repositories.StockRepository;
import java.util.List;
import com.azurion.shared.api.PageRequestSupport;
import com.azurion.shared.api.PageResponse;
import org.springframework.data.domain.Sort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListStockUseCase {

    private static final int LEGACY_MAX_SIZE = 200;

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

        return page(null, null, 0, LEGACY_MAX_SIZE).content();
    }

    public List<StockResponse> executeBySucursal(Long sucursalId) {
        return stockRepository.findByAlmacenSucursalId(sucursalId).stream()
                .map(stockMovimientoUseCase::toStockResponse)
                .toList();
    }

    public PageResponse<StockResponse> page(
            Long productoId,
            Long almacenId,
            int page,
            int size
    ) {
        var result = stockRepository.search(
                productoId,
                almacenId,
                PageRequestSupport.of(
                        page,
                        size,
                        Sort.by("producto.nombre").ascending().and(Sort.by("almacen.nombre").ascending())
                )
        );
        return PageResponse.from(
                result,
                result.getContent().stream().map(stockMovimientoUseCase::toStockResponse).toList()
        );
    }
}
