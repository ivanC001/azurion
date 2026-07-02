package com.azurion.saascore.inventory.application.usecases;

import com.azurion.saascore.inventory.application.dto.ProductoResponse;
import com.azurion.saascore.inventory.application.dto.UpdateProductoRequest;
import com.azurion.saascore.inventory.application.mappers.ProductoInventoryMapper;
import com.azurion.saascore.inventory.domain.entities.Producto;
import com.azurion.saascore.inventory.domain.entities.Stock;
import com.azurion.saascore.inventory.domain.repositories.CategoriaRepository;
import com.azurion.saascore.inventory.domain.repositories.MarcaRepository;
import com.azurion.saascore.inventory.domain.repositories.ProductoRepository;
import com.azurion.saascore.inventory.domain.repositories.StockRepository;
import com.azurion.saascore.inventory.domain.repositories.UnidadMedidaRepository;
import com.azurion.saascore.tributacion.application.services.TaxConfigurationValidator;
import com.azurion.shared.exception.BusinessException;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateProductoUseCase {

    private final ProductoRepository productoRepository;
    private final StockRepository stockRepository;
    private final CategoriaRepository categoriaRepository;
    private final MarcaRepository marcaRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;
    private final TaxConfigurationValidator taxConfigurationValidator;

    @Transactional
    public ProductoResponse execute(Long productoId, UpdateProductoRequest request) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new BusinessException("PRODUCTO_NO_ENCONTRADO", "Producto no encontrado"));

        BigDecimal precio = request.precio() == null ? producto.getPrecio() : request.precio();
        if (precio.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("PRECIO_INVALIDO", "El precio no puede ser negativo");
        }

        String codigo = defaultIfBlank(request.codigo(), producto.getCodigo()).toUpperCase();
        if (productoRepository.existsByCodigoIgnoreCaseAndIdNot(codigo, productoId)) {
            throw new BusinessException("CODIGO_PRODUCTO_DUPLICADO", "Ya existe otro producto con ese codigo");
        }

        producto.setNombre(request.nombre().trim());
        producto.setPrecio(precio);
        producto.setCodigo(codigo);
        producto.setCodigoBarras(trim(request.codigoBarras()));
        producto.setDescripcion(trim(request.descripcion()));
        if (request.categoriaId() != null) {
            producto.setCategoria(categoriaRepository.findById(request.categoriaId())
                    .orElseThrow(() -> new BusinessException("CATEGORIA_NO_ENCONTRADA", "Categoria no encontrada")));
        }
        if (request.marcaId() != null) {
            producto.setMarca(marcaRepository.findById(request.marcaId())
                    .orElseThrow(() -> new BusinessException("MARCA_NO_ENCONTRADA", "Marca no encontrada")));
        }
        if (request.unidadMedidaId() != null) {
            producto.setUnidadMedida(unidadMedidaRepository.findById(request.unidadMedidaId())
                    .orElseThrow(() -> new BusinessException("UNIDAD_MEDIDA_NO_ENCONTRADA", "Unidad de medida no encontrada")));
        }
        producto.setTipoProducto(defaultIfBlank(request.tipoProducto(), producto.getTipoProducto()).toUpperCase());
        producto.setImagenUrl(trim(defaultIfBlank(request.imagenUrl(), defaultIfBlank(request.foto(), producto.getImagenUrl()))));
        producto.setFoto(trim(defaultIfBlank(request.foto(), defaultIfBlank(request.imagenUrl(), producto.getFoto()))));
        producto.setPrecioCompraBase(request.precioCompraBase() == null ? producto.getPrecioCompraBase() : request.precioCompraBase());
        producto.setPrecioVentaBase(request.precioVentaBase() == null ? producto.getPrecioVentaBase() : request.precioVentaBase());
        producto.setCostoPromedio(resolveCostoPromedio(producto, request));
        producto.setAfectoIgv(request.afectoIgv() == null ? producto.isAfectoIgv() : request.afectoIgv());
        boolean usaConfiguracionEmpresa = request.usaConfiguracionEmpresa() == null
                ? producto.isUsaConfiguracionEmpresa()
                : request.usaConfiguracionEmpresa();
        if (!usaConfiguracionEmpresa) {
            taxConfigurationValidator.validateProducto(producto.isAfectoIgv(), request.tipoAfectacionIgvId(), request.tributoId(), request.porcentajeImpuesto());
        }
        producto.setUsaConfiguracionEmpresa(usaConfiguracionEmpresa);
        producto.setTipoAfectacionIgvId(request.tipoAfectacionIgvId() == null ? producto.getTipoAfectacionIgvId() : trim(request.tipoAfectacionIgvId()));
        producto.setTributoId(request.tributoId() == null ? producto.getTributoId() : trim(request.tributoId()));
        producto.setPorcentajeImpuesto(request.porcentajeImpuesto() == null ? producto.getPorcentajeImpuesto() : request.porcentajeImpuesto());
        boolean manejaVencimiento = resolveBoolean(request.vencimiento(), request.manejaVencimiento(), producto.isManejaVencimiento());
        producto.setManejaVencimiento(manejaVencimiento);
        producto.setManejaLotes(manejaVencimiento || resolveBoolean(request.lotes(), request.manejaLotes(), producto.isManejaLotes()));
        producto.setManejaStock(resolveBoolean(request.stock(), request.manejaStock(), producto.isManejaStock()));
        if ("SERVICIO".equalsIgnoreCase(producto.getTipoProducto())) {
            producto.setManejaStock(false);
            producto.setManejaLotes(false);
            producto.setManejaVencimiento(false);
        }
        BigDecimal stockMinimo = request.stockMinimo() == null ? request.stockMinimoGlobal() : request.stockMinimo();
        validateStockMinimo(stockMinimo);
        producto.setStockMinimoGlobal(stockMinimo == null ? producto.getStockMinimoGlobal() : stockMinimo);
        producto.setStockMinimo(stockMinimo == null ? producto.getStockMinimo() : stockMinimo);
        boolean activo = request.activo() == null ? producto.isActivo() : request.activo();
        producto.setEstado(defaultIfBlank(request.estado(), activo ? "ACTIVO" : "INACTIVO").toUpperCase());
        producto.setActivo(activo);
        Producto saved = productoRepository.save(producto);
        ensureInitialStock(saved);

        BigDecimal stockTotal = stockRepository.findByProductoId(saved.getId()).stream()
                .map(stock -> stock.getCantidad() == null ? BigDecimal.ZERO : stock.getCantidad())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ProductoInventoryMapper.toResponse(saved, stockTotal);
    }

    private void ensureInitialStock(Producto producto) {
        if (!producto.isManejaStock() || !stockRepository.findByProductoId(producto.getId()).isEmpty()) {
            return;
        }
        Stock stock = new Stock();
        stock.setProducto(producto);
        stock.setAlmacen(producto.getAlmacen());
        stock.setCantidad(BigDecimal.ZERO);
        stock.setStockReservado(BigDecimal.ZERO);
        stock.setStockMinimo(BigDecimal.ZERO);
        stock.setEstado("ACTIVO");
        stockRepository.save(stock);
    }

    private void validateStockMinimo(BigDecimal stockMinimo) {
        if (stockMinimo != null && stockMinimo.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("STOCK_MINIMO_INVALIDO", "El stock minimo no puede ser negativo");
        }
    }

    private BigDecimal resolveCostoPromedio(Producto producto, UpdateProductoRequest request) {
        if (request.costoPromedio() != null) {
            return request.costoPromedio();
        }
        if (request.precioCompraBase() != null) {
            return request.precioCompraBase();
        }
        return producto.getCostoPromedio();
    }

    private boolean resolveBoolean(Boolean preferred, Boolean fallback, boolean defaultValue) {
        if (preferred != null) {
            return preferred;
        }
        if (fallback != null) {
            return fallback;
        }
        return defaultValue;
    }

    private String defaultIfBlank(String value, String fallback) {
        String trimmed = trim(value);
        return trimmed == null ? fallback : trimmed;
    }

    private String trim(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }
}
