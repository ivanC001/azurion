package com.azurion.saascore.inventory.application.usecases;

import com.azurion.saascore.auth.application.services.AuthorizationService;
import com.azurion.saascore.inventory.application.dto.StockResponse;
import com.azurion.saascore.inventory.application.dto.UpdateStockSettingsRequest;
import com.azurion.saascore.inventory.domain.entities.Stock;
import com.azurion.saascore.inventory.domain.repositories.StockRepository;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateStockSettingsUseCase {

    private final StockRepository stockRepository;
    private final StockMovimientoUseCase stockMovimientoUseCase;
    private final AuthorizationService authorizationService;

    @Transactional
    public StockResponse execute(Long stockId, UpdateStockSettingsRequest request) {
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new BusinessException("STOCK_NO_ENCONTRADO", "Registro de stock no encontrado"));
        authorizationService.validarAlmacen(
                authorizationService.currentUsuarioId(),
                stock.getAlmacen().getId()
        );
        if (request.stockMaximo() != null
                && request.stockMaximo().compareTo(request.stockMinimo()) < 0) {
            throw new BusinessException(
                    "STOCK_MAXIMO_INVALIDO",
                    "El stock maximo no puede ser menor al stock minimo"
            );
        }
        stock.setStockMinimo(request.stockMinimo());
        stock.setStockMaximo(request.stockMaximo());
        stock.setUbicacionFisica(trim(request.ubicacionFisica()));
        return stockMovimientoUseCase.toStockResponse(stockRepository.save(stock));
    }

    private String trim(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }
}
