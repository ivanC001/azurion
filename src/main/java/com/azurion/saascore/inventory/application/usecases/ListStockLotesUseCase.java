package com.azurion.saascore.inventory.application.usecases;

import com.azurion.saascore.inventory.application.dto.StockLoteResponse;
import com.azurion.saascore.inventory.domain.entities.StockLote;
import com.azurion.saascore.inventory.domain.repositories.StockLoteRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.azurion.shared.api.PageRequestSupport;
import com.azurion.shared.api.PageResponse;
import org.springframework.data.domain.Sort;

@Service
@RequiredArgsConstructor
public class ListStockLotesUseCase {

    private static final int LEGACY_MAX_SIZE = 200;

    private final StockLoteRepository stockLoteRepository;

    @Transactional(readOnly = true)
    public List<StockLoteResponse> execute(Long productoId, Long almacenId) {
        List<StockLote> rows;
        if (productoId != null && almacenId != null) {
            rows = stockLoteRepository.findByProductoIdAndAlmacenIdOrderByLoteFechaVencimientoAscLoteFechaIngresoAsc(productoId, almacenId);
        } else if (productoId != null) {
            rows = stockLoteRepository.findByProductoIdOrderByLoteFechaVencimientoAscLoteFechaIngresoAsc(productoId);
        } else if (almacenId != null) {
            rows = stockLoteRepository.findByAlmacenIdOrderByLoteFechaVencimientoAscLoteFechaIngresoAsc(almacenId);
        } else {
            return page(null, null, 0, LEGACY_MAX_SIZE).content();
        }
        return rows.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<StockLoteResponse> page(
            Long productoId,
            Long almacenId,
            int page,
            int size
    ) {
        var result = stockLoteRepository.search(
                productoId,
                almacenId,
                PageRequestSupport.of(
                        page,
                        size,
                        Sort.by("lote.fechaVencimiento").ascending()
                                .and(Sort.by("lote.fechaIngreso").ascending())
                )
        );
        return PageResponse.from(result, result.getContent().stream().map(this::toResponse).toList());
    }

    private StockLoteResponse toResponse(StockLote stockLote) {
        return new StockLoteResponse(
                stockLote.getId(),
                stockLote.getLote().getId(),
                stockLote.getLote().getCodigoLote(),
                stockLote.getProducto().getId(),
                stockLote.getProducto().getSku(),
                stockLote.getProducto().getNombre(),
                stockLote.getAlmacen().getId(),
                stockLote.getAlmacen().getCodigo(),
                stockLote.getAlmacen().getNombre(),
                stockLote.getStockActual(),
                stockLote.getLote().getFechaIngreso(),
                stockLote.getLote().getFechaVencimiento(),
                stockLote.getEstado()
        );
    }
}
