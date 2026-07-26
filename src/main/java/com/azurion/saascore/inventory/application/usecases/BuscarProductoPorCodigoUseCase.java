package com.azurion.saascore.inventory.application.usecases;

import com.azurion.saascore.inventory.application.dto.ProductoResponse;
import com.azurion.saascore.inventory.application.mappers.ProductoInventoryMapper;
import com.azurion.saascore.inventory.domain.entities.Producto;
import com.azurion.saascore.inventory.domain.repositories.ProductoRepository;
import com.azurion.saascore.inventory.domain.repositories.StockRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BuscarProductoPorCodigoUseCase {

    private final ProductoRepository productoRepository;
    private final StockRepository stockRepository;

    @Transactional(readOnly = true)
    public ProductoResponse execute(String rawCode) {
        String code = rawCode == null ? "" : rawCode.trim();
        if (code.isEmpty()) {
            return null;
        }

        Producto producto = productoRepository.findByCodigoBarrasIgnoreCase(code)
                .or(() -> productoRepository.findBySkuIgnoreCase(code))
                .or(() -> productoRepository.findByCodigoIgnoreCase(code))
                .orElse(null);
        if (producto == null) {
            return null;
        }

        BigDecimal stock = stockRepository.sumCantidadByProductoId(producto.getId());
        return ProductoInventoryMapper.toResponse(producto, stock);
    }
}
