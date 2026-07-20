package com.azurion.saascore.inventory.application.usecases;

import com.azurion.saascore.inventory.application.dto.ProductoResponse;
import com.azurion.saascore.inventory.application.mappers.ProductoInventoryMapper;
import com.azurion.saascore.inventory.domain.entities.Stock;
import com.azurion.saascore.inventory.domain.repositories.ProductoRepository;
import com.azurion.saascore.inventory.domain.repositories.StockRepository;
import com.azurion.shared.api.PageRequestSupport;
import com.azurion.shared.api.PageResponse;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;

@Service
@RequiredArgsConstructor
public class ListProductosUseCase {

    private final ProductoRepository productoRepository;
    private final StockRepository stockRepository;

    @Transactional(readOnly = true)
    public List<ProductoResponse> execute(Long almacenId) {
        return page("", almacenId, 0, PageRequestSupport.MAX_SIZE).content();
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductoResponse> page(String query, Long almacenId, int page, int size) {
        var productos = productoRepository.search(
                query == null ? "" : query.trim(),
                almacenId,
                PageRequestSupport.of(page, size, Sort.by("nombre").ascending())
        );

        List<Long> productoIds = productos.getContent().stream().map(producto -> producto.getId()).toList();

        Map<Long, BigDecimal> stockByProducto = new HashMap<>();
        if (!productoIds.isEmpty()) {
            for (Object[] row : stockRepository.sumCantidadByProductoIds(productoIds, almacenId)) {
                stockByProducto.put((Long) row[0], row[1] == null ? BigDecimal.ZERO : (BigDecimal) row[1]);
            }
        }

        List<ProductoResponse> content = productos.getContent().stream()
                .map(p -> ProductoInventoryMapper.toResponse(p, stockByProducto.getOrDefault(p.getId(), BigDecimal.ZERO)))
                .toList();
        return PageResponse.from(productos, content);
    }
}
