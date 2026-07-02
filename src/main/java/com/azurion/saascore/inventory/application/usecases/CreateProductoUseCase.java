package com.azurion.saascore.inventory.application.usecases;

import com.azurion.saascore.almacenes.domain.entities.Almacen;
import com.azurion.saascore.almacenes.domain.repositories.AlmacenRepository;
import com.azurion.saascore.inventory.application.dto.CreateProductoRequest;
import com.azurion.saascore.inventory.application.dto.ProductoResponse;
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
public class CreateProductoUseCase {

    private final ProductoRepository productoRepository;
    private final StockRepository stockRepository;
    private final AlmacenRepository almacenRepository;
    private final CategoriaRepository categoriaRepository;
    private final MarcaRepository marcaRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;
    private final TaxConfigurationValidator taxConfigurationValidator;

    @Transactional
    public ProductoResponse execute(CreateProductoRequest request) {
        String sku = request.sku().trim().toUpperCase();
        String nombre = request.nombre().trim();
        BigDecimal precio = request.precio() == null ? BigDecimal.ZERO : request.precio();
        if (precio.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("PRECIO_INVALIDO", "El precio no puede ser negativo");
        }

        productoRepository.findBySkuIgnoreCase(sku).ifPresent(existing -> {
            throw new BusinessException("PRODUCTO_DUPLICADO", "Ya existe un producto con ese SKU");
        });
        String codigo = defaultIfBlank(request.codigo(), sku).toUpperCase();
        if (productoRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new BusinessException("CODIGO_PRODUCTO_DUPLICADO", "Ya existe un producto con ese codigo");
        }

        Almacen almacen = resolveAlmacen(request.almacenId());

        Producto producto = new Producto();
        producto.setSku(sku);
        producto.setCodigo(codigo);
        producto.setCodigoBarras(trim(request.codigoBarras()));
        producto.setNombre(nombre);
        producto.setDescripcion(trim(request.descripcion()));
        producto.setPrecio(precio);
        producto.setAlmacen(almacen);
        producto.setTipoProducto(defaultIfBlank(request.tipoProducto(), "PRODUCTO").toUpperCase());
        producto.setImagenUrl(trim(defaultIfBlank(request.imagenUrl(), request.foto())));
        producto.setFoto(trim(defaultIfBlank(request.foto(), request.imagenUrl())));
        producto.setPrecioCompraBase(request.precioCompraBase() == null ? BigDecimal.ZERO : request.precioCompraBase());
        producto.setPrecioVentaBase(request.precioVentaBase() == null ? precio : request.precioVentaBase());
        producto.setCostoPromedio(resolveCostoPromedio(request));
        producto.setAfectoIgv(request.afectoIgv() == null || request.afectoIgv());
        boolean usaConfiguracionEmpresa = request.usaConfiguracionEmpresa() == null || request.usaConfiguracionEmpresa();
        if (!usaConfiguracionEmpresa) {
            taxConfigurationValidator.validateProducto(producto.isAfectoIgv(), request.tipoAfectacionIgvId(), request.tributoId(), request.porcentajeImpuesto());
        }
        producto.setUsaConfiguracionEmpresa(usaConfiguracionEmpresa);
        producto.setTipoAfectacionIgvId(trim(request.tipoAfectacionIgvId()));
        producto.setTributoId(trim(request.tributoId()));
        producto.setPorcentajeImpuesto(request.porcentajeImpuesto());
        boolean manejaVencimiento = resolveBoolean(request.vencimiento(), request.manejaVencimiento(), false);
        producto.setManejaVencimiento(manejaVencimiento);
        producto.setManejaLotes(manejaVencimiento || resolveBoolean(request.lotes(), request.manejaLotes(), false));
        producto.setManejaStock(resolveBoolean(request.stock(), request.manejaStock(), true));
        if ("SERVICIO".equalsIgnoreCase(producto.getTipoProducto())) {
            producto.setManejaStock(false);
            producto.setManejaLotes(false);
            producto.setManejaVencimiento(false);
        }
        BigDecimal stockMinimo = request.stockMinimo() == null ? request.stockMinimoGlobal() : request.stockMinimo();
        validateStockMinimo(stockMinimo);
        producto.setStockMinimoGlobal(stockMinimo == null ? BigDecimal.ZERO : stockMinimo);
        producto.setStockMinimo(stockMinimo == null ? BigDecimal.ZERO : stockMinimo);
        producto.setEstado("ACTIVO");
        producto.setActivo(true);
        if (request.categoriaId() != null) {
            producto.setCategoria(categoriaRepository.findById(request.categoriaId())
                    .orElseThrow(() -> new BusinessException("CATEGORIA_NO_ENCONTRADA", "Categoria no encontrada")));
        }
        if (request.marcaId() != null) {
            producto.setMarca(marcaRepository.findById(request.marcaId())
                    .orElseThrow(() -> new BusinessException("MARCA_NO_ENCONTRADA", "Marca no encontrada")));
        }
        producto.setUnidadMedida(resolveUnidadMedida(request.unidadMedidaId()));

        Producto saved = productoRepository.save(producto);
        createInitialStock(saved, almacen);
        return ProductoInventoryMapper.toResponse(saved, BigDecimal.ZERO);
    }

    private void createInitialStock(Producto producto, Almacen almacen) {
        if (!producto.isManejaStock()) {
            return;
        }
        Stock stock = new Stock();
        stock.setProducto(producto);
        stock.setAlmacen(almacen);
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

    private BigDecimal resolveCostoPromedio(CreateProductoRequest request) {
        if (request.costoPromedio() != null) {
            return request.costoPromedio();
        }
        if (request.precioCompraBase() != null) {
            return request.precioCompraBase();
        }
        return BigDecimal.ZERO;
    }

    private Almacen resolveAlmacen(Long almacenId) {
        if (almacenId != null) {
            return almacenRepository.findById(almacenId)
                    .orElseThrow(() -> new BusinessException("ALMACEN_NO_ENCONTRADO", "Almacen no encontrado"));
        }
        return almacenRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException("ALMACEN_REQUERIDO", "Registra al menos un almacen antes de crear productos"));
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

    private com.azurion.saascore.inventory.domain.entities.UnidadMedida resolveUnidadMedida(Long unidadMedidaId) {
        if (unidadMedidaId != null) {
            return unidadMedidaRepository.findById(unidadMedidaId)
                    .orElseThrow(() -> new BusinessException("UNIDAD_MEDIDA_NO_ENCONTRADA", "Unidad de medida no encontrada"));
        }
        return unidadMedidaRepository.findByCodigoSunat("NIU").orElse(null);
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
