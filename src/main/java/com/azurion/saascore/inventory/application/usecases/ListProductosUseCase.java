package com.azurion.saascore.inventory.application.usecases;

import com.azurion.saascore.inventory.application.dto.ProductoResponse;
import com.azurion.saascore.inventory.application.mappers.ProductoInventoryMapper;
import com.azurion.saascore.inventory.domain.entities.Stock;
import com.azurion.saascore.inventory.domain.repositories.ProductoRepository;
import com.azurion.saascore.inventory.domain.repositories.StockRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListProductosUseCase {

    private final ProductoRepository productoRepository;
    private final StockRepository stockRepository;

    @Transactional(readOnly = true)
    public List<ProductoResponse> execute(Long almacenId) {
        var productos = productoRepository.findAllByOrderByNombreAsc();

        List<Stock> stocks = (almacenId == null)
                ? stockRepository.findAll()
                : stockRepository.findByAlmacenId(almacenId);

        Map<Long, BigDecimal> stockByProducto = new HashMap<>();
        for (Stock stock : stocks) {
            Long productoId = stock.getProducto().getId();
            BigDecimal cantidad = stock.getCantidad() == null ? BigDecimal.ZERO : stock.getCantidad();
            stockByProducto.merge(productoId, cantidad, BigDecimal::add);
        }

        return productos.stream()
                .map(p -> ProductoInventoryMapper.toResponse(p, stockByProducto.getOrDefault(p.getId(), BigDecimal.ZERO)))
                .toList();
    }
}
