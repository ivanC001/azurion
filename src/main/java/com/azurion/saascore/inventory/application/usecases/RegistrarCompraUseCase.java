package com.azurion.saascore.inventory.application.usecases;

import com.azurion.saascore.almacenes.domain.entities.Almacen;
import com.azurion.saascore.almacenes.domain.repositories.AlmacenRepository;
import com.azurion.saascore.inventory.application.dto.CompraDetalleRequest;
import com.azurion.saascore.inventory.application.dto.CompraResponse;
import com.azurion.saascore.inventory.application.dto.CreateCompraRequest;
import com.azurion.saascore.inventory.application.mappers.CompraInventoryMapper;
import com.azurion.saascore.inventory.domain.entities.Compra;
import com.azurion.saascore.inventory.domain.entities.CompraDetalle;
import com.azurion.saascore.inventory.domain.entities.KardexMovimiento;
import com.azurion.saascore.inventory.domain.entities.Lote;
import com.azurion.saascore.inventory.domain.entities.Producto;
import com.azurion.saascore.inventory.domain.entities.Stock;
import com.azurion.saascore.inventory.domain.entities.StockLote;
import com.azurion.saascore.inventory.domain.repositories.CompraDetalleRepository;
import com.azurion.saascore.inventory.domain.repositories.CompraRepository;
import com.azurion.saascore.inventory.domain.repositories.KardexMovimientoRepository;
import com.azurion.saascore.inventory.domain.repositories.LoteRepository;
import com.azurion.saascore.inventory.domain.repositories.ProductoRepository;
import com.azurion.saascore.inventory.domain.repositories.StockLoteRepository;
import com.azurion.saascore.inventory.domain.repositories.StockRepository;
import com.azurion.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrarCompraUseCase {

    private final CompraRepository compraRepository;
    private final CompraDetalleRepository compraDetalleRepository;
    private final ProductoRepository productoRepository;
    private final AlmacenRepository almacenRepository;
    private final StockRepository stockRepository;
    private final LoteRepository loteRepository;
    private final StockLoteRepository stockLoteRepository;
    private final KardexMovimientoRepository kardexRepository;

    @Transactional
    public CompraResponse execute(CreateCompraRequest request) {
        if (request.detalles() == null || request.detalles().isEmpty()) {
            throw new BusinessException("COMPRA_SIN_DETALLES", "La compra debe tener al menos un detalle");
        }

        String tipoComprobante = normalizeTipoComprobante(request.tipoComprobante());
        String numeroComprobante = defaultIfBlank(request.numeroComprobante(), buildNumeroComprobante(request.serie(), request.correlativo()));
        validateComprobanteUnicoPorProveedor(request, tipoComprobante, numeroComprobante);

        Almacen almacen = almacenRepository.findById(request.almacenId())
                .orElseThrow(() -> new BusinessException("ALMACEN_NO_ENCONTRADO", "Almacen destino no encontrado"));
        if (!almacen.isActivo() || !"ACTIVO".equalsIgnoreCase(almacen.getEstado())) {
            throw new BusinessException("ALMACEN_INACTIVO", "No se puede ingresar una compra en un almacen inactivo");
        }
        if (almacen.getSucursal() == null || !almacen.getSucursal().isActivo()) {
            throw new BusinessException("SUCURSAL_INACTIVA", "La sucursal del almacen esta inactiva y no permite ingresos");
        }

        Map<Long, Producto> productosBloqueados = lockProductos(request.detalles());

        Compra compra = new Compra();
        compra.setProveedorId(request.proveedorId());
        compra.setProveedorDocumento(trim(request.proveedorDocumento()));
        compra.setProveedorNombre(trim(request.proveedorNombre()));
        compra.setTipoComprobante(tipoComprobante);
        compra.setSerie(trim(request.serie()));
        compra.setCorrelativo(trim(request.correlativo()));
        compra.setNumeroComprobante(numeroComprobante);
        compra.setFechaEmision(request.fechaEmision());
        compra.setFechaIngreso(request.fechaIngreso() == null ? OffsetDateTime.now() : request.fechaIngreso());
        compra.setAlmacen(almacen);
        compra.setEstado("REGISTRADA");
        compra.setTotal(BigDecimal.ZERO);
        Compra savedCompra = compraRepository.save(compra);

        List<CompraDetalle> detalles = new ArrayList<>();
        BigDecimal totalCompra = BigDecimal.ZERO;
        for (CompraDetalleRequest detalleRequest : request.detalles()) {
            CompraDetalle detalle = registrarDetalle(savedCompra, almacen, detalleRequest, productosBloqueados);
            detalles.add(detalle);
            totalCompra = totalCompra.add(detalle.getTotal());
        }

        savedCompra.setTotal(totalCompra.setScale(2, RoundingMode.HALF_UP));
        Compra compraConTotal = compraRepository.save(savedCompra);
        return CompraInventoryMapper.toResponse(compraConTotal, detalles);
    }

    private CompraDetalle registrarDetalle(
            Compra compra,
            Almacen almacen,
            CompraDetalleRequest request,
            Map<Long, Producto> productosBloqueados
    ) {
        Producto producto = productosBloqueados.get(request.productoId());

        BigDecimal cantidad = positive(request.cantidad(), "DETALLE_CANTIDAD_INVALIDA", "La cantidad debe ser mayor a cero");
        BigDecimal costoUnitario = positive(request.costoUnitario(), "DETALLE_COSTO_INVALIDO", "El costo unitario debe ser mayor a cero");
        BigDecimal precioVenta = resolvePrecioVenta(producto, request.precioVenta());
        BigDecimal total = cantidad.multiply(costoUnitario).setScale(2, RoundingMode.HALF_UP);

        CompraDetalle detalle = new CompraDetalle();
        detalle.setCompra(compra);
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        detalle.setCostoUnitario(costoUnitario);
        detalle.setPrecioVenta(precioVenta);
        detalle.setTotal(total);
        detalle.setCodigoLote(trim(request.codigoLote()));
        detalle.setFechaFabricacion(request.fechaFabricacion());
        detalle.setFechaVencimiento(request.fechaVencimiento());
        CompraDetalle savedDetalle = compraDetalleRepository.save(detalle);

        Lote lote = resolveLote(producto, compra, savedDetalle, request, cantidad, costoUnitario);
        Stock stock = resolveStock(producto, almacen);
        BigDecimal saldoAnterior = stock.getCantidad();
        BigDecimal saldoNuevo = saldoAnterior.add(cantidad);
        stock.setCantidad(saldoNuevo);
        stockRepository.save(stock);

        if (lote != null) {
            StockLote stockLote = resolveStockLote(lote, producto, almacen);
            stockLote.setStockActual(stockLote.getStockActual().add(cantidad));
            stockLoteRepository.save(stockLote);
        }

        actualizarCostosProducto(producto, cantidad, saldoAnterior, saldoNuevo, costoUnitario, precioVenta);
        registrarKardex(compra, savedDetalle, producto, almacen, lote, cantidad, saldoAnterior, saldoNuevo, costoUnitario, precioVenta);
        return savedDetalle;
    }

    private Map<Long, Producto> lockProductos(List<CompraDetalleRequest> detalles) {
        Map<Long, Producto> productos = new LinkedHashMap<>();
        detalles.stream()
                .map(CompraDetalleRequest::productoId)
                .distinct()
                .sorted()
                .forEach(productoId -> productos.put(
                        productoId,
                        productoRepository.findByIdForUpdate(productoId)
                                .orElseThrow(() -> new BusinessException(
                                        "PRODUCTO_NO_ENCONTRADO",
                                        "Producto no encontrado: " + productoId
                                ))
                ));
        return productos;
    }

    private BigDecimal resolvePrecioVenta(Producto producto, BigDecimal precioVenta) {
        BigDecimal resolved = precioVenta;
        if (resolved == null || resolved.compareTo(BigDecimal.ZERO) <= 0) {
            resolved = producto.getPrecioVentaBase() == null ? producto.getPrecio() : producto.getPrecioVentaBase();
        }
        if (resolved == null || resolved.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("DETALLE_PRECIO_VENTA_INVALIDO", "Indica un precio de venta mayor a cero para calcular la rentabilidad");
        }
        return resolved;
    }

    private Lote resolveLote(
            Producto producto,
            Compra compra,
            CompraDetalle detalle,
            CompraDetalleRequest request,
            BigDecimal cantidad,
            BigDecimal costoUnitario
    ) {
        String codigoLote = trim(request.codigoLote());
        if (codigoLote == null && request.fechaVencimiento() == null && request.fechaFabricacion() == null) {
            return null;
        }
        if (codigoLote == null) {
            throw new BusinessException("LOTE_CODIGO_REQUERIDO", "Debe indicar codigoLote para registrar lote");
        }

        producto.setManejaLotes(true);
        if (request.fechaVencimiento() != null) {
            producto.setManejaVencimiento(true);
        }
        productoRepository.save(producto);

        return loteRepository.findByProductoIdAndCodigoLote(producto.getId(), codigoLote)
                .map(existing -> {
                    if (existing.getCompraDetalle() == null) {
                        existing.setCompraDetalle(detalle);
                    }
                    existing.setCantidadInicial(existing.getCantidadInicial().add(cantidad));
                    existing.setCostoUnitario(costoUnitario);
                    return loteRepository.save(existing);
                })
                .orElseGet(() -> {
                    Lote lote = new Lote();
                    lote.setProducto(producto);
                    lote.setCompraDetalle(detalle);
                    lote.setCodigoLote(codigoLote);
                    lote.setFechaFabricacion(request.fechaFabricacion());
                    lote.setFechaIngreso(compra.getFechaIngreso().toLocalDate());
                    lote.setFechaVencimiento(request.fechaVencimiento());
                    lote.setProveedorId(compra.getProveedorId());
                    lote.setCantidadInicial(cantidad);
                    lote.setCostoUnitario(costoUnitario);
                    lote.setEstado("ACTIVO");
                    return loteRepository.save(lote);
                });
    }

    private Stock resolveStock(Producto producto, Almacen almacen) {
        return stockRepository.findByProductoIdAndAlmacenId(producto.getId(), almacen.getId())
                .orElseGet(() -> {
                    Stock stock = new Stock();
                    stock.setProducto(producto);
                    stock.setAlmacen(almacen);
                    stock.setCantidad(BigDecimal.ZERO);
                    stock.setStockReservado(BigDecimal.ZERO);
                    stock.setStockMinimo(producto.getStockMinimoGlobal() == null ? BigDecimal.ZERO : producto.getStockMinimoGlobal());
                    stock.setEstado("ACTIVO");
                    return stock;
                });
    }

    private StockLote resolveStockLote(Lote lote, Producto producto, Almacen almacen) {
        return stockLoteRepository.findByLoteIdAndAlmacenId(lote.getId(), almacen.getId())
                .orElseGet(() -> {
                    StockLote stockLote = new StockLote();
                    stockLote.setLote(lote);
                    stockLote.setProducto(producto);
                    stockLote.setAlmacen(almacen);
                    stockLote.setStockActual(BigDecimal.ZERO);
                    stockLote.setEstado("ACTIVO");
                    return stockLote;
                });
    }

    private void actualizarCostosProducto(
            Producto producto,
            BigDecimal cantidad,
            BigDecimal saldoAnterior,
            BigDecimal saldoNuevo,
            BigDecimal costoUnitario,
            BigDecimal precioVenta
    ) {
        if (saldoNuevo.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal costoAnterior = producto.getCostoPromedio() == null ? BigDecimal.ZERO : producto.getCostoPromedio();
            BigDecimal costoPromedio = costoAnterior.multiply(saldoAnterior)
                    .add(costoUnitario.multiply(cantidad))
                    .divide(saldoNuevo, 6, RoundingMode.HALF_UP);
            producto.setCostoPromedio(costoPromedio);
        }
        producto.setPrecioCompraBase(costoUnitario);
        if (precioVenta != null && precioVenta.compareTo(BigDecimal.ZERO) > 0) {
            producto.setPrecioVentaBase(precioVenta);
            producto.setPrecio(precioVenta);
        }
        productoRepository.save(producto);
    }

    private void registrarKardex(
            Compra compra,
            CompraDetalle detalle,
            Producto producto,
            Almacen almacen,
            Lote lote,
            BigDecimal cantidad,
            BigDecimal saldoAnterior,
            BigDecimal saldoNuevo,
            BigDecimal costoUnitario,
            BigDecimal precioVenta
    ) {
        KardexMovimiento movimiento = new KardexMovimiento();
        movimiento.setProducto(producto);
        movimiento.setAlmacen(almacen);
        movimiento.setLote(lote);
        movimiento.setTipoMovimiento("ENTRADA");
        movimiento.setMotivo("COMPRA");
        movimiento.setReferenciaTipo("COMPRA");
        movimiento.setReferenciaId(compra.getId());
        movimiento.setReferencia(compra.getNumeroComprobante());
        movimiento.setCantidad(cantidad);
        movimiento.setStockAnterior(saldoAnterior);
        movimiento.setStockNuevo(saldoNuevo);
        movimiento.setSaldoResultante(saldoNuevo);
        movimiento.setCostoUnitario(costoUnitario);
        movimiento.setCostoTotal(costoUnitario.multiply(cantidad));
        movimiento.setPrecioCompra(costoUnitario);
        movimiento.setPrecioVenta(precioVenta);
        movimiento.setFechaMovimiento(compra.getFechaIngreso());
        movimiento.setObservacion("Compra detalle " + detalle.getId());
        kardexRepository.save(movimiento);
    }

    private BigDecimal positive(BigDecimal value, String code, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(code, message);
        }
        return value;
    }

    private String normalizeTipoComprobante(String value) {
        String normalized = defaultIfBlank(value, "OTRO").toUpperCase();
        return switch (normalized) {
            case "FACTURA", "BOLETA", "TICKET", "OTRO" -> normalized;
            default -> throw new BusinessException("COMPRA_TIPO_INVALIDO", "Use FACTURA, BOLETA, TICKET u OTRO");
        };
    }

    private void validateComprobanteUnicoPorProveedor(CreateCompraRequest request, String tipoComprobante, String numeroComprobante) {
        String providerKey = proveedorKey(request.proveedorId(), request.proveedorDocumento());
        boolean duplicated = compraRepository.findByNumeroComprobanteIgnoreCase(numeroComprobante).stream()
                .anyMatch(existing -> tipoComprobante.equalsIgnoreCase(existing.getTipoComprobante())
                        && providerKey.equals(proveedorKey(existing.getProveedorId(), existing.getProveedorDocumento())));
        if (duplicated) {
            throw new BusinessException("COMPRA_DUPLICADA", "Ya existe ese comprobante para el proveedor indicado");
        }
    }

    private String proveedorKey(Long proveedorId, String proveedorDocumento) {
        String documento = trim(proveedorDocumento);
        if (documento != null) {
            return documento.toUpperCase();
        }
        return proveedorId == null ? "SIN_PROVEEDOR" : proveedorId.toString();
    }

    private String buildNumeroComprobante(String serie, String correlativo) {
        String serieValue = trim(serie);
        String correlativoValue = trim(correlativo);
        if (serieValue == null || correlativoValue == null) {
            return null;
        }
        return serieValue + "-" + correlativoValue;
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
