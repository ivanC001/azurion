package com.azurion.saascore.inventory.application.usecases;

import com.azurion.saascore.almacenes.domain.entities.Almacen;
import com.azurion.saascore.almacenes.domain.repositories.AlmacenRepository;
import com.azurion.saascore.inventory.application.dto.ProductoResponse;
import com.azurion.saascore.inventory.application.dto.UpdateProductoRequest;
import com.azurion.saascore.inventory.application.mappers.ProductoInventoryMapper;
import com.azurion.saascore.inventory.application.services.InventoryOperationalValidator;
import com.azurion.saascore.inventory.application.services.ProductoPhotoValidator;
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
    private final AlmacenRepository almacenRepository;
    private final CategoriaRepository categoriaRepository;
    private final MarcaRepository marcaRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;
    private final TaxConfigurationValidator taxConfigurationValidator;
    private final InventoryOperationalValidator inventoryValidator;

    @Transactional
    public ProductoResponse execute(Long productoId, UpdateProductoRequest request) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new BusinessException("PRODUCTO_NO_ENCONTRADO", "Producto no encontrado"));

        BigDecimal precio = request.precio() == null ? producto.getPrecio() : request.precio();
        if (precio.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("PRECIO_INVALIDO", "El precio no puede ser negativo");
        }
        validateMoney(request.precioCompraBase(), "PRECIO_COMPRA_INVALIDO", "El precio de compra no puede ser negativo");
        validateMoney(request.precioVentaBase(), "PRECIO_VENTA_INVALIDO", "El precio de venta no puede ser negativo");
        validateMoney(request.costoPromedio(), "COSTO_PROMEDIO_INVALIDO", "El costo promedio no puede ser negativo");

        String codigo = defaultIfBlank(request.codigo(), producto.getCodigo()).toUpperCase();
        if (productoRepository.existsByCodigoIgnoreCaseAndIdNot(codigo, productoId)) {
            throw new BusinessException("CODIGO_PRODUCTO_DUPLICADO", "Ya existe otro producto con ese codigo");
        }
        String codigoBarras = trim(request.codigoBarras());
        if (codigoBarras != null
                && productoRepository.existsByCodigoBarrasIgnoreCaseAndIdNot(codigoBarras, productoId)) {
            throw new BusinessException(
                    "CODIGO_BARRAS_DUPLICADO",
                    "Ya existe otro producto con ese codigo de barras"
            );
        }

        producto.setNombre(request.nombre().trim());
        producto.setPrecio(precio);
        producto.setCodigo(codigo);
        producto.setCodigoBarras(codigoBarras);
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
        String tipoProducto = normalizeTipoProducto(request.tipoProducto(), producto.getTipoProducto());
        if ("SERVICIO".equals(tipoProducto)
                && stockRepository.sumCantidadByProductoId(producto.getId()).compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException(
                    "PRODUCTO_CON_STOCK",
                    "No se puede convertir en servicio un producto que todavia tiene existencias"
            );
        }
        producto.setTipoProducto(tipoProducto);
        if ("SERVICIO".equals(tipoProducto)) {
            producto.setAlmacen(null);
        } else if (request.almacenId() != null) {
            Almacen almacen = almacenRepository.findById(request.almacenId())
                    .orElseThrow(() -> new BusinessException(
                            "ALMACEN_NO_ENCONTRADO",
                            "Almacen no encontrado"
                    ));
            boolean cambioAlmacen = producto.getAlmacen() == null
                    || !producto.getAlmacen().getId().equals(almacen.getId());
            if (cambioAlmacen || Boolean.TRUE.equals(request.activo())) {
                inventoryValidator.requireOperationalWarehouse(almacen);
            }
            producto.setAlmacen(almacen);
        } else if (producto.getAlmacen() == null) {
            throw new BusinessException(
                    "ALMACEN_REQUERIDO",
                    "Selecciona el almacen inicial del producto"
            );
        }
        String currentPhoto = defaultIfBlank(producto.getFoto(), producto.getImagenUrl());
        String requestedPhoto = defaultIfBlank(request.foto(), request.imagenUrl());
        String photo = ProductoPhotoValidator.preserveOrValidate(requestedPhoto, currentPhoto);
        producto.setImagenUrl(photo);
        producto.setFoto(photo);
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
        if (!"SERVICIO".equalsIgnoreCase(producto.getTipoProducto())
                && producto.getCategoria() == null) {
            throw new BusinessException(
                    "CATEGORIA_REQUERIDA",
                    "Selecciona una categoria para el producto"
            );
        }
        BigDecimal stockMinimo = request.stockMinimo() == null ? request.stockMinimoGlobal() : request.stockMinimo();
        validateStockMinimo(stockMinimo);
        producto.setStockMinimoGlobal(stockMinimo == null ? producto.getStockMinimoGlobal() : stockMinimo);
        producto.setStockMinimo(stockMinimo == null ? producto.getStockMinimo() : stockMinimo);
        boolean activo = request.activo() == null ? producto.isActivo() : request.activo();
        BigDecimal stockActual = stockRepository.sumCantidadByProductoId(producto.getId());
        if (!activo && stockActual.compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException(
                    "PRODUCTO_CON_STOCK",
                    "Deja el producto sin existencias antes de desactivarlo"
            );
        }
        producto.setEstado(activo ? "ACTIVO" : "INACTIVO");
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
        if (producto.getAlmacen() == null) {
            throw new BusinessException(
                    "ALMACEN_REQUERIDO",
                    "Asigna un almacen al producto antes de activar el control de stock"
            );
        }
        Stock stock = new Stock();
        stock.setProducto(producto);
        stock.setAlmacen(producto.getAlmacen());
        stock.setCantidad(BigDecimal.ZERO);
        stock.setStockReservado(BigDecimal.ZERO);
        stock.setStockMinimo(
                producto.getStockMinimoGlobal() == null
                        ? BigDecimal.ZERO
                        : producto.getStockMinimoGlobal()
        );
        stock.setEstado("ACTIVO");
        stockRepository.save(stock);
    }

    private void validateStockMinimo(BigDecimal stockMinimo) {
        if (stockMinimo != null && stockMinimo.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("STOCK_MINIMO_INVALIDO", "El stock minimo no puede ser negativo");
        }
    }

    private void validateMoney(BigDecimal value, String code, String message) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(code, message);
        }
    }

    private String normalizeTipoProducto(String value, String fallback) {
        String tipo = defaultIfBlank(value, fallback).toUpperCase();
        if (!"PRODUCTO".equals(tipo) && !"SERVICIO".equals(tipo)) {
            throw new BusinessException(
                    "TIPO_PRODUCTO_INVALIDO",
                    "El tipo de registro debe ser PRODUCTO o SERVICIO"
            );
        }
        return tipo;
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
