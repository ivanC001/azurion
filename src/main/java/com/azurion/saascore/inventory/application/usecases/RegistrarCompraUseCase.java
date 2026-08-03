package com.azurion.saascore.inventory.application.usecases;

import com.azurion.saascore.almacenes.domain.entities.Almacen;
import com.azurion.saascore.almacenes.domain.repositories.AlmacenRepository;
import com.azurion.saascore.inventory.application.dto.CompraDetalleRequest;
import com.azurion.saascore.inventory.application.dto.CompraResponse;
import com.azurion.saascore.inventory.application.dto.CreateCompraRequest;
import com.azurion.saascore.inventory.application.mappers.CompraInventoryMapper;
import com.azurion.saascore.inventory.application.services.InventoryOperationalValidator;
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
import com.azurion.saascore.tributacion.application.dto.TaxResolution;
import com.azurion.saascore.tributacion.application.services.TaxResolverService;
import com.azurion.saascore.tributacion.domain.entities.ConfiguracionTributariaEmpresa;
import com.azurion.shared.exception.BusinessException;
import com.azurion.shared.persistence.BusinessOperationLockService;
import com.azurion.shared.util.RequestFingerprint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrarCompraUseCase {

    private static final Set<String> AFECTACIONES_GRAVADAS = Set.of(
            "10", "11", "12", "13", "14", "15", "16", "17"
    );

    private final CompraRepository compraRepository;
    private final CompraDetalleRepository compraDetalleRepository;
    private final ProductoRepository productoRepository;
    private final AlmacenRepository almacenRepository;
    private final StockRepository stockRepository;
    private final LoteRepository loteRepository;
    private final StockLoteRepository stockLoteRepository;
    private final KardexMovimientoRepository kardexRepository;
    private final InventoryOperationalValidator inventoryValidator;
    private final BusinessOperationLockService operationLockService;
    private final TaxResolverService taxResolverService;

    @Transactional
    public CompraResponse execute(CreateCompraRequest request) {
        if (request.detalles() == null || request.detalles().isEmpty()) {
            throw new BusinessException("COMPRA_SIN_DETALLES", "La compra debe tener al menos un detalle");
        }
        validateProveedor(request);

        String tipoComprobante = normalizeTipoComprobante(request.tipoComprobante());
        if (Boolean.TRUE.equals(request.creditoFiscalAplicable()) && !"FACTURA".equals(tipoComprobante)) {
            throw new BusinessException(
                    "COMPRA_CREDITO_FISCAL_INVALIDO",
                    "Solo una factura de compra puede marcarse con derecho a credito fiscal"
            );
        }
        String numeroComprobante = defaultIfBlank(request.numeroComprobante(), buildNumeroComprobante(request.serie(), request.correlativo()));
        if (numeroComprobante == null) {
            throw new BusinessException(
                    "COMPRA_COMPROBANTE_REQUERIDO",
                    "Indica el numero de comprobante de la compra"
            );
        }
        String operationKey = normalizeOperationKey(request.clientOperationId());
        operationLockService.lockAll(List.of(
                "purchase-document:" + tipoComprobante + ':'
                        + proveedorKey(request.proveedorId(), request.proveedorDocumento()) + ':'
                        + numeroComprobante.toUpperCase(),
                operationKey == null ? "" : "purchase-operation:" + operationKey
        ));

        Almacen almacen = almacenRepository.findById(request.almacenId())
                .orElseThrow(() -> new BusinessException("ALMACEN_NO_ENCONTRADO", "Almacen destino no encontrado"));
        inventoryValidator.requireOperationalWarehouse(almacen);
        ConfiguracionTributariaEmpresa taxEmpresa = taxResolverService.configuracionEmpresa();

        Map<Long, Producto> productosBloqueados = lockProductos(request.detalles());
        String requestHash = operationKey == null ? null : RequestFingerprint.sha256(request);
        if (operationKey != null) {
            Compra completed = compraRepository.findByClientOperationId(operationKey).orElse(null);
            if (completed != null) {
                if (!completed.getRequestHash().equals(requestHash)) {
                    throw new BusinessException(
                            "OPERACION_COMPRA_REUTILIZADA",
                            "El identificador de operacion ya fue usado con datos diferentes"
                    );
                }
                return CompraInventoryMapper.toResponse(completed, completed.getDetalles());
            }
        }
        // Lock first and check the natural document key afterwards so two
        // concurrent purchases cannot both pass the duplicate check.
        validateComprobanteUnicoPorProveedor(request, tipoComprobante, numeroComprobante);

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
        compra.setSubtotalNeto(BigDecimal.ZERO);
        compra.setMontoIgv(BigDecimal.ZERO);
        compra.setCreditoFiscalAplicable(Boolean.TRUE.equals(request.creditoFiscalAplicable()));
        compra.setTotalCostoInventariable(BigDecimal.ZERO);
        compra.setTratamientoIgv("DESGLOSADO");
        compra.setClientOperationId(operationKey);
        compra.setRequestHash(requestHash);
        Compra savedCompra = operationKey == null
                ? compraRepository.save(compra)
                : compraRepository.saveAndFlush(compra);

        List<CompraDetalle> detalles = new ArrayList<>();
        BigDecimal totalCompra = BigDecimal.ZERO;
        BigDecimal subtotalNeto = BigDecimal.ZERO;
        BigDecimal montoIgv = BigDecimal.ZERO;
        BigDecimal totalCostoInventariable = BigDecimal.ZERO;
        for (CompraDetalleRequest detalleRequest : request.detalles()) {
            CompraDetalle detalle = registrarDetalle(
                    savedCompra,
                    almacen,
                    detalleRequest,
                    productosBloqueados,
                    taxEmpresa
            );
            detalles.add(detalle);
            totalCompra = totalCompra.add(detalle.getTotal());
            subtotalNeto = subtotalNeto.add(detalle.getSubtotalNeto());
            montoIgv = montoIgv.add(detalle.getMontoIgv());
            totalCostoInventariable = totalCostoInventariable.add(detalle.getTotalCostoInventariable());
        }

        savedCompra.setTotal(totalCompra.setScale(2, RoundingMode.HALF_UP));
        savedCompra.setSubtotalNeto(subtotalNeto.setScale(2, RoundingMode.HALF_UP));
        savedCompra.setMontoIgv(montoIgv.setScale(2, RoundingMode.HALF_UP));
        savedCompra.setTotalCostoInventariable(totalCostoInventariable.setScale(2, RoundingMode.HALF_UP));
        Compra compraConTotal = compraRepository.save(savedCompra);
        return CompraInventoryMapper.toResponse(compraConTotal, detalles);
    }

    private CompraDetalle registrarDetalle(
            Compra compra,
            Almacen almacen,
            CompraDetalleRequest request,
            Map<Long, Producto> productosBloqueados,
            ConfiguracionTributariaEmpresa taxEmpresa
    ) {
        Producto producto = productosBloqueados.get(request.productoId());
        inventoryValidator.requireStockProduct(producto);
        inventoryValidator.validateLotDates(request.fechaFabricacion(), request.fechaVencimiento());

        BigDecimal cantidad = positive(request.cantidad(), "DETALLE_CANTIDAD_INVALIDA", "La cantidad debe ser mayor a cero");
        PurchaseLineAmounts amounts = resolvePurchaseLineAmounts(compra, request, cantidad);
        BigDecimal costoUnitario = amounts.costoInventariableUnitario();
        BigDecimal precioVenta = resolvePrecioVenta(producto, request.precioVenta());
        BigDecimal precioVentaNeto = resolvePrecioVentaNeto(
                producto,
                almacen,
                taxEmpresa,
                precioVenta
        );

        CompraDetalle detalle = new CompraDetalle();
        detalle.setCompra(compra);
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        detalle.setCostoUnitario(costoUnitario);
        detalle.setCostoNetoUnitario(amounts.costoNetoUnitario());
        detalle.setPorcentajeIgv(amounts.porcentajeIgv());
        detalle.setMontoIgvUnitario(amounts.montoIgvUnitario());
        detalle.setCostoTotalUnitario(amounts.costoTotalUnitario());
        detalle.setCostoInventariableUnitario(costoUnitario);
        detalle.setPrecioVenta(precioVenta);
        detalle.setPrecioVentaNeto(precioVentaNeto);
        detalle.setSubtotalNeto(amounts.subtotalNeto());
        detalle.setMontoIgv(amounts.montoIgv());
        detalle.setTotal(amounts.total());
        detalle.setTotalCostoInventariable(amounts.totalCostoInventariable());
        detalle.setCodigoLote(trim(request.codigoLote()));
        detalle.setFechaFabricacion(request.fechaFabricacion());
        detalle.setFechaVencimiento(request.fechaVencimiento());
        CompraDetalle savedDetalle = compraDetalleRepository.save(detalle);

        Lote lote = resolveLote(producto, compra, savedDetalle, request, cantidad, costoUnitario);
        Stock stock = resolveStock(producto, almacen);
        BigDecimal saldoAnterior = stock.getCantidad();
        BigDecimal saldoNuevo = saldoAnterior.add(cantidad);
        BigDecimal saldoGlobalAnterior = stockRepository.sumCantidadByProductoId(producto.getId());
        BigDecimal saldoGlobalNuevo = saldoGlobalAnterior.add(cantidad);
        stock.setCantidad(saldoNuevo);
        stockRepository.save(stock);

        if (lote != null) {
            StockLote stockLote = resolveStockLote(lote, producto, almacen);
            stockLote.setStockActual(stockLote.getStockActual().add(cantidad));
            stockLoteRepository.save(stockLote);
        }

        actualizarCostosProducto(
                producto,
                cantidad,
                saldoGlobalAnterior,
                saldoGlobalNuevo,
                costoUnitario,
                precioVenta
        );
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

    private PurchaseLineAmounts resolvePurchaseLineAmounts(
            Compra compra,
            CompraDetalleRequest request,
            BigDecimal cantidad
    ) {
        // Backwards compatibility: older clients only sent costoUnitario and
        // that value was already used as the inventory cost. New clients send
        // costoNetoUnitario explicitly so IGV can be separated safely.
        if (request.costoNetoUnitario() == null) {
            BigDecimal legacyCost = positive(
                    request.costoUnitario(),
                    "DETALLE_COSTO_INVALIDO",
                    "El costo unitario debe ser mayor a cero"
            ).setScale(6, RoundingMode.HALF_UP);
            BigDecimal lineTotal = legacyCost.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
            return new PurchaseLineAmounts(
                    legacyCost,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP),
                    legacyCost,
                    legacyCost,
                    lineTotal,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    lineTotal,
                    lineTotal
            );
        }

        BigDecimal netUnit = positive(
                request.costoNetoUnitario(),
                "DETALLE_COSTO_NETO_INVALIDO",
                "El costo neto unitario debe ser mayor a cero"
        ).setScale(6, RoundingMode.HALF_UP);
        BigDecimal rate = request.porcentajeIgv() == null
                ? BigDecimal.ZERO
                : request.porcentajeIgv().setScale(2, RoundingMode.HALF_UP);
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException(
                    "DETALLE_IGV_INVALIDO",
                    "El porcentaje de IGV de compra debe estar entre 0 y 100"
            );
        }

        BigDecimal taxUnit = netUnit.multiply(rate)
                .divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
        BigDecimal totalUnit = netUnit.add(taxUnit).setScale(6, RoundingMode.HALF_UP);
        if (request.costoTotalUnitario() != null
                && request.costoTotalUnitario().subtract(totalUnit).abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw new BusinessException(
                    "DETALLE_TOTAL_COMPRA_INVALIDO",
                    "El costo total unitario no coincide con el costo neto mas IGV"
            );
        }

        BigDecimal netLine = netUnit.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxLine = netLine.multiply(rate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal totalLine = netLine.add(taxLine).setScale(2, RoundingMode.HALF_UP);
        BigDecimal inventoryUnit = compra.isCreditoFiscalAplicable() && rate.compareTo(BigDecimal.ZERO) > 0
                ? netUnit
                : totalUnit;
        BigDecimal inventoryLine = inventoryUnit.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
        return new PurchaseLineAmounts(
                netUnit,
                rate,
                taxUnit,
                totalUnit,
                inventoryUnit,
                netLine,
                taxLine,
                totalLine,
                inventoryLine
        );
    }

    private BigDecimal resolvePrecioVentaNeto(
            Producto producto,
            Almacen almacen,
            ConfiguracionTributariaEmpresa taxEmpresa,
            BigDecimal precioVentaFinal
    ) {
        TaxResolution tax = taxResolverService.resolverImpuesto(
                producto,
                almacen.getSucursal(),
                taxEmpresa
        );
        if (!AFECTACIONES_GRAVADAS.contains(tax.tipoAfectacionCodigo())
                || tax.porcentajeIgv().compareTo(BigDecimal.ZERO) <= 0) {
            return precioVentaFinal.setScale(6, RoundingMode.HALF_UP);
        }
        BigDecimal factor = BigDecimal.ONE.add(
                tax.porcentajeIgv().divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP)
        );
        return precioVentaFinal.divide(factor, 6, RoundingMode.HALF_UP);
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
        boolean controlaLotes = producto.isManejaLotes() || producto.isManejaVencimiento();
        boolean tieneDatosLote = codigoLote != null
                || request.fechaVencimiento() != null
                || request.fechaFabricacion() != null;
        if (!controlaLotes && !tieneDatosLote) {
            return null;
        }
        if (codigoLote == null) {
            throw new BusinessException(
                    "LOTE_CODIGO_REQUERIDO",
                    "Debe indicar el codigo de lote para este producto"
            );
        }
        if (producto.isManejaVencimiento() && request.fechaVencimiento() == null) {
            throw new BusinessException(
                    "LOTE_VENCIMIENTO_REQUERIDO",
                    "Debe indicar la fecha de vencimiento para este producto"
            );
        }

        producto.setManejaLotes(true);
        if (request.fechaVencimiento() != null) {
            producto.setManejaVencimiento(true);
        }
        productoRepository.save(producto);

        return loteRepository.findByProductoIdAndCodigoLote(producto.getId(), codigoLote)
                .map(existing -> {
                    validateExistingLot(existing, request);
                    if (existing.getCompraDetalle() == null) {
                        existing.setCompraDetalle(detalle);
                    }
                    BigDecimal cantidadAnterior = existing.getCantidadInicial();
                    BigDecimal cantidadNueva = cantidadAnterior.add(cantidad);
                    BigDecimal costoPonderado = existing.getCostoUnitario()
                            .multiply(cantidadAnterior)
                            .add(costoUnitario.multiply(cantidad))
                            .divide(cantidadNueva, 6, RoundingMode.HALF_UP);
                    existing.setCantidadInicial(cantidadNueva);
                    existing.setCostoUnitario(costoPonderado);
                    if (existing.getFechaFabricacion() == null) {
                        existing.setFechaFabricacion(request.fechaFabricacion());
                    }
                    if (existing.getFechaVencimiento() == null) {
                        existing.setFechaVencimiento(request.fechaVencimiento());
                    }
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

    private void validateProveedor(CreateCompraRequest request) {
        if (request.proveedorId() == null
                && trim(request.proveedorDocumento()) == null
                && trim(request.proveedorNombre()) == null) {
            throw new BusinessException(
                    "PROVEEDOR_REQUERIDO",
                    "Indica el documento o nombre del proveedor"
            );
        }
    }

    private void validateExistingLot(Lote existing, CompraDetalleRequest request) {
        if (existing.getFechaFabricacion() != null
                && request.fechaFabricacion() != null
                && !existing.getFechaFabricacion().equals(request.fechaFabricacion())) {
            throw new BusinessException(
                    "LOTE_FABRICACION_DIFERENTE",
                    "El lote ya existe con una fecha de fabricacion diferente"
            );
        }
        if (existing.getFechaVencimiento() != null
                && request.fechaVencimiento() != null
                && !existing.getFechaVencimiento().equals(request.fechaVencimiento())) {
            throw new BusinessException(
                    "LOTE_VENCIMIENTO_DIFERENTE",
                    "El lote ya existe con una fecha de vencimiento diferente"
            );
        }
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

    private String normalizeOperationKey(String value) {
        String operationKey = trim(value);
        if (operationKey != null && operationKey.length() > 100) {
            throw new BusinessException(
                    "OPERACION_COMPRA_INVALIDA",
                    "El identificador de operacion no puede superar 100 caracteres"
            );
        }
        return operationKey;
    }

    private String trim(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record PurchaseLineAmounts(
            BigDecimal costoNetoUnitario,
            BigDecimal porcentajeIgv,
            BigDecimal montoIgvUnitario,
            BigDecimal costoTotalUnitario,
            BigDecimal costoInventariableUnitario,
            BigDecimal subtotalNeto,
            BigDecimal montoIgv,
            BigDecimal total,
            BigDecimal totalCostoInventariable
    ) {
    }
}
