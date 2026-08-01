package com.azurion.saascore.inventory.application.usecases;

import com.azurion.saascore.inventory.application.dto.CreateProductoRapidoRequest;
import com.azurion.saascore.inventory.application.dto.CreateProductoRequest;
import com.azurion.saascore.inventory.application.dto.ProductoResponse;
import com.azurion.saascore.inventory.application.dto.StockMovimientoRequest;
import com.azurion.saascore.inventory.application.mappers.ProductoInventoryMapper;
import com.azurion.saascore.inventory.application.services.ProductoSkuGenerator;
import com.azurion.saascore.inventory.domain.entities.Producto;
import com.azurion.saascore.inventory.domain.repositories.ProductoRepository;
import com.azurion.saascore.inventory.domain.repositories.StockRepository;
import com.azurion.shared.exception.BusinessException;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateProductoRapidoUseCase {

    private final CreateProductoUseCase createProductoUseCase;
    private final StockMovimientoUseCase stockMovimientoUseCase;
    private final ProductoSkuGenerator skuGenerator;
    private final ProductoRepository productoRepository;
    private final StockRepository stockRepository;

    @Transactional
    public ProductoResponse execute(CreateProductoRapidoRequest request) {
        BigDecimal cantidadInicial = zeroIfNull(request.cantidadInicial());
        BigDecimal costoInicial = zeroIfNull(request.costoInicial());
        String tipoProducto = defaultIfBlank(request.tipoProducto(), "PRODUCTO").toUpperCase();

        if ("SERVICIO".equals(tipoProducto) && cantidadInicial.signum() > 0) {
            throw new BusinessException(
                    "SERVICIO_STOCK_INVALIDO",
                    "Un servicio no puede registrarse con cantidad inicial"
            );
        }
        if (cantidadInicial.signum() > 0 && costoInicial.signum() <= 0) {
            throw new BusinessException(
                    "COSTO_INICIAL_REQUERIDO",
                    "Indica un costo inicial mayor a cero cuando registres existencias"
            );
        }
        if (Boolean.TRUE.equals(request.manejaVencimiento())
                && cantidadInicial.signum() > 0
                && (request.fechaVencimiento() == null || isBlank(request.codigoLote()))) {
            throw new BusinessException(
                    "LOTE_INICIAL_INCOMPLETO",
                    "Indica el lote y la fecha de vencimiento para registrar las existencias iniciales"
            );
        }

        String sku = isBlank(request.sku())
                ? nextAvailableSku()
                : request.sku().trim().toUpperCase();

        ProductoResponse created = createProductoUseCase.execute(new CreateProductoRequest(
                sku,
                request.nombre(),
                request.precioVenta(),
                request.almacenId(),
                sku,
                trim(request.codigoBarras()),
                trim(request.descripcion()),
                request.categoriaId(),
                null,
                request.unidadMedidaId(),
                tipoProducto,
                request.foto(),
                request.foto(),
                costoInicial,
                request.precioVenta(),
                costoInicial,
                true,
                null,
                null,
                null,
                true,
                !"SERVICIO".equals(tipoProducto),
                Boolean.TRUE.equals(request.manejaVencimiento()),
                Boolean.TRUE.equals(request.manejaVencimiento()),
                !"SERVICIO".equals(tipoProducto),
                Boolean.TRUE.equals(request.manejaVencimiento()),
                Boolean.TRUE.equals(request.manejaVencimiento()),
                request.stockMinimo(),
                request.stockMinimo()
        ));

        if (cantidadInicial.signum() > 0) {
            stockMovimientoUseCase.execute(new StockMovimientoRequest(
                    created.id(),
                    request.almacenId(),
                    null,
                    null,
                    trim(request.codigoLote()),
                    request.fechaFabricacion(),
                    request.fechaVencimiento(),
                    "ENTRADA",
                    "STOCK_INICIAL",
                    cantidadInicial,
                    costoInicial,
                    costoInicial,
                    request.precioVenta(),
                    null,
                    "Alta rapida de producto",
                    null
            ));
        }

        Producto producto = productoRepository.findById(created.id())
                .orElseThrow(() -> new BusinessException("PRODUCTO_NO_ENCONTRADO", "Producto no encontrado"));
        return ProductoInventoryMapper.toResponse(
                producto,
                stockRepository.sumCantidadByProductoId(producto.getId())
        );
    }

    private String nextAvailableSku() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = skuGenerator.nextSku();
            if (productoRepository.findBySkuIgnoreCase(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new BusinessException("SKU_NO_GENERADO", "No se pudo generar un SKU unico para el producto");
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String defaultIfBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trim(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
