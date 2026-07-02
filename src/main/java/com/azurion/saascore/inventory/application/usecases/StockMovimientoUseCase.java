package com.azurion.saascore.inventory.application.usecases;

import com.azurion.saascore.almacenes.domain.entities.Almacen;
import com.azurion.saascore.auth.application.services.AuthorizationService;
import com.azurion.saascore.almacenes.domain.repositories.AlmacenRepository;
import com.azurion.saascore.inventory.application.dto.KardexMovimientoResponse;
import com.azurion.saascore.inventory.application.dto.StockMovimientoRequest;
import com.azurion.saascore.inventory.application.dto.StockResponse;
import com.azurion.saascore.inventory.domain.entities.KardexMovimiento;
import com.azurion.saascore.inventory.domain.entities.Lote;
import com.azurion.saascore.inventory.domain.entities.Producto;
import com.azurion.saascore.inventory.domain.entities.Stock;
import com.azurion.saascore.inventory.domain.entities.StockLote;
import com.azurion.saascore.inventory.domain.repositories.KardexMovimientoRepository;
import com.azurion.saascore.inventory.domain.repositories.LoteRepository;
import com.azurion.saascore.inventory.domain.repositories.ProductoRepository;
import com.azurion.saascore.inventory.domain.repositories.StockLoteRepository;
import com.azurion.saascore.inventory.domain.repositories.StockRepository;
import com.azurion.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockMovimientoUseCase {

    private final ProductoRepository productoRepository;
    private final AlmacenRepository almacenRepository;
    private final StockRepository stockRepository;
    private final LoteRepository loteRepository;
    private final StockLoteRepository stockLoteRepository;
    private final KardexMovimientoRepository kardexRepository;
    private final AuthorizationService authorizationService;

    @Transactional
    public KardexMovimientoResponse execute(StockMovimientoRequest request) {
        Long usuarioId = authorizationService.currentUsuarioId();
        authorizationService.validarAlmacen(usuarioId, request.almacenId());
        if (request.almacenDestinoId() != null) {
            authorizationService.validarAlmacen(usuarioId, request.almacenDestinoId());
        }
        Producto producto = productoRepository.findById(request.productoId())
                .orElseThrow(() -> new BusinessException("PRODUCTO_NO_ENCONTRADO", "Producto no encontrado"));
        validateProductoMovible(producto);

        Almacen almacen = almacenRepository.findById(request.almacenId())
                .orElseThrow(() -> new BusinessException("ALMACEN_NO_ENCONTRADO", "Almacen no encontrado"));
        validateAlmacenOperativo(almacen);

        String tipo = normalizeTipo(request.tipoMovimiento());
        BigDecimal cantidad = normalizeCantidad(request.cantidad());

        return switch (tipo) {
            case "ENTRADA" -> registrarEntrada(producto, almacen, request, cantidad);
            case "SALIDA" -> registrarSalida(producto, almacen, request, cantidad);
            case "AJUSTE" -> registrarAjuste(producto, almacen, request, cantidad);
            case "TRASLADO" -> registrarTraslado(producto, almacen, request, cantidad);
            default -> throw new BusinessException("TIPO_MOVIMIENTO_INVALIDO", "Use ENTRADA, SALIDA, AJUSTE o TRASLADO");
        };
    }

    private KardexMovimientoResponse registrarEntrada(Producto producto, Almacen almacen, StockMovimientoRequest request, BigDecimal cantidad) {
        Lote lote = shouldRegistrarLote(producto, request) ? resolveOrCreateLote(prepareProductoParaLote(producto, request), request) : null;
        Stock stock = resolveStock(producto, almacen);
        BigDecimal saldoAnterior = stock.getCantidad();
        BigDecimal nuevoSaldo = saldoAnterior.add(cantidad);
        stock.setCantidad(nuevoSaldo);
        stockRepository.save(stock);
        actualizarCostosProductoPorEntrada(producto, request, cantidad, saldoAnterior, nuevoSaldo);

        if (lote != null) {
            StockLote stockLote = resolveStockLote(lote, producto, almacen);
            stockLote.setStockActual(stockLote.getStockActual().add(cantidad));
            stockLoteRepository.save(stockLote);
        }

        KardexMovimiento saved = saveMovimiento(producto, almacen, lote, "ENTRADA", request, cantidad, saldoAnterior, nuevoSaldo);
        return toKardexResponse(saved);
    }

    private boolean shouldRegistrarLote(Producto producto, StockMovimientoRequest request) {
        return producto.isManejaLotes()
                || request.loteId() != null
                || trim(request.codigoLote()) != null
                || request.fechaFabricacion() != null
                || request.fechaVencimiento() != null;
    }

    private Producto prepareProductoParaLote(Producto producto, StockMovimientoRequest request) {
        boolean changed = false;
        if (!producto.isManejaLotes()) {
            producto.setManejaLotes(true);
            changed = true;
        }
        if (request.fechaVencimiento() != null && !producto.isManejaVencimiento()) {
            producto.setManejaVencimiento(true);
            changed = true;
        }
        return changed ? productoRepository.save(producto) : producto;
    }

    private KardexMovimientoResponse registrarSalida(Producto producto, Almacen almacen, StockMovimientoRequest request, BigDecimal cantidad) {
        Stock stock = resolveStock(producto, almacen);
        BigDecimal saldoAnterior = stock.getCantidad();
        BigDecimal nuevoSaldo = saldoAnterior.subtract(cantidad);
        if (nuevoSaldo.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("STOCK_INSUFICIENTE", "No hay stock suficiente para salida");
        }

        Lote firstAffectedLote = null;
        if (producto.isManejaLotes()) {
            firstAffectedLote = descontarStockLotes(producto, almacen, request.loteId(), cantidad);
        }

        stock.setCantidad(nuevoSaldo);
        stockRepository.save(stock);
        KardexMovimiento saved = saveMovimiento(producto, almacen, firstAffectedLote, "SALIDA", request, cantidad, saldoAnterior, nuevoSaldo);
        return toKardexResponse(saved);
    }

    private KardexMovimientoResponse registrarAjuste(Producto producto, Almacen almacen, StockMovimientoRequest request, BigDecimal nuevoSaldo) {
        Stock stock = resolveStock(producto, almacen);
        BigDecimal saldoAnterior = stock.getCantidad();
        BigDecimal diferencia = nuevoSaldo.subtract(saldoAnterior);
        String tipoKardex = diferencia.compareTo(BigDecimal.ZERO) >= 0 ? "AJUSTE_POSITIVO" : "AJUSTE_NEGATIVO";

        if (producto.isManejaLotes()) {
            Lote lote = resolveRequiredExistingLote(producto, request.loteId(), "El ajuste de producto con lotes debe indicar loteId");
            StockLote stockLote = resolveStockLote(lote, producto, almacen);
            BigDecimal loteNuevoSaldo = stockLote.getStockActual().add(diferencia);
            if (loteNuevoSaldo.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("STOCK_LOTE_INSUFICIENTE", "El ajuste dejaria stock negativo en el lote");
            }
            stockLote.setStockActual(loteNuevoSaldo);
            stockLoteRepository.save(stockLote);
        }

        stock.setCantidad(nuevoSaldo);
        stockRepository.save(stock);
        KardexMovimiento saved = saveMovimiento(producto, almacen, null, tipoKardex, request, diferencia.abs(), saldoAnterior, nuevoSaldo);
        return toKardexResponse(saved);
    }

    private KardexMovimientoResponse registrarTraslado(Producto producto, Almacen almacenOrigen, StockMovimientoRequest request, BigDecimal cantidad) {
        Long almacenDestinoId = request.almacenDestinoId();
        if (almacenDestinoId == null) {
            throw new BusinessException("ALMACEN_DESTINO_REQUERIDO", "Debe indicar almacenDestinoId para traslados");
        }
        if (almacenDestinoId.equals(almacenOrigen.getId())) {
            throw new BusinessException("ALMACEN_DESTINO_INVALIDO", "El almacen destino debe ser distinto al origen");
        }

        Almacen almacenDestino = almacenRepository.findById(almacenDestinoId)
                .orElseThrow(() -> new BusinessException("ALMACEN_DESTINO_NO_ENCONTRADO", "Almacen destino no encontrado"));
        validateAlmacenOperativo(almacenDestino);

        Stock stockOrigen = resolveStock(producto, almacenOrigen);
        BigDecimal origenAnterior = stockOrigen.getCantidad();
        BigDecimal origenNuevo = origenAnterior.subtract(cantidad);
        if (origenNuevo.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("STOCK_INSUFICIENTE", "No hay stock suficiente para traslado");
        }

        Lote firstAffectedLote = null;
        if (producto.isManejaLotes()) {
            firstAffectedLote = trasladarStockLotes(producto, almacenOrigen, almacenDestino, request.loteId(), cantidad);
        }

        Stock stockDestino = resolveStock(producto, almacenDestino);
        BigDecimal destinoAnterior = stockDestino.getCantidad();
        BigDecimal destinoNuevo = destinoAnterior.add(cantidad);

        stockOrigen.setCantidad(origenNuevo);
        stockDestino.setCantidad(destinoNuevo);
        stockRepository.save(stockOrigen);
        stockRepository.save(stockDestino);

        KardexMovimiento salida = saveMovimiento(producto, almacenOrigen, firstAffectedLote, "TRANSFERENCIA_SALIDA", request, cantidad, origenAnterior, origenNuevo);
        saveMovimiento(producto, almacenDestino, firstAffectedLote, "TRANSFERENCIA_ENTRADA", request, cantidad, destinoAnterior, destinoNuevo);
        return toKardexResponse(salida);
    }

    private Lote descontarStockLotes(Producto producto, Almacen almacen, Long loteId, BigDecimal cantidad) {
        List<StockLote> candidatos = resolveStockLoteCandidates(producto, almacen, loteId);
        BigDecimal pendiente = cantidad;
        Lote firstAffected = null;

        for (StockLote stockLote : candidatos) {
            validateLoteVigente(stockLote.getLote());
            BigDecimal disponible = stockLote.getStockActual();
            if (disponible.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal consumir = disponible.min(pendiente);
            stockLote.setStockActual(disponible.subtract(consumir));
            stockLoteRepository.save(stockLote);
            if (firstAffected == null) {
                firstAffected = stockLote.getLote();
            }
            pendiente = pendiente.subtract(consumir);
            if (pendiente.compareTo(BigDecimal.ZERO) <= 0) {
                return firstAffected;
            }
        }

        throw new BusinessException("STOCK_LOTE_INSUFICIENTE", "No hay stock suficiente por lote para el producto");
    }

    private Lote trasladarStockLotes(Producto producto, Almacen origen, Almacen destino, Long loteId, BigDecimal cantidad) {
        List<StockLote> candidatos = resolveStockLoteCandidates(producto, origen, loteId);
        BigDecimal pendiente = cantidad;
        Lote firstAffected = null;

        for (StockLote stockOrigen : candidatos) {
            validateLoteVigente(stockOrigen.getLote());
            BigDecimal disponible = stockOrigen.getStockActual();
            if (disponible.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal mover = disponible.min(pendiente);
            stockOrigen.setStockActual(disponible.subtract(mover));
            stockLoteRepository.save(stockOrigen);

            StockLote stockDestino = resolveStockLote(stockOrigen.getLote(), producto, destino);
            stockDestino.setStockActual(stockDestino.getStockActual().add(mover));
            stockLoteRepository.save(stockDestino);

            if (firstAffected == null) {
                firstAffected = stockOrigen.getLote();
            }
            pendiente = pendiente.subtract(mover);
            if (pendiente.compareTo(BigDecimal.ZERO) <= 0) {
                return firstAffected;
            }
        }

        throw new BusinessException("STOCK_LOTE_INSUFICIENTE", "No hay stock suficiente por lote para trasladar");
    }

    private List<StockLote> resolveStockLoteCandidates(Producto producto, Almacen almacen, Long loteId) {
        List<StockLote> candidates = loteId == null
                ? stockLoteRepository.findByProductoIdAndAlmacenIdOrderByLoteFechaVencimientoAscLoteFechaIngresoAsc(producto.getId(), almacen.getId())
                : stockLoteRepository.findByLoteIdAndAlmacenId(loteId, almacen.getId()).stream().toList();

        Comparator<StockLote> comparator = producto.isManejaVencimiento()
                ? Comparator
                        .comparing((StockLote item) -> item.getLote().getFechaVencimiento(), Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(item -> item.getLote().getFechaIngreso(), Comparator.nullsLast(Comparator.naturalOrder()))
                : Comparator.comparing(item -> item.getLote().getFechaIngreso(), Comparator.nullsLast(Comparator.naturalOrder()));

        return candidates.stream()
                .filter(item -> "ACTIVO".equalsIgnoreCase(item.getEstado()))
                .filter(item -> "ACTIVO".equalsIgnoreCase(item.getLote().getEstado()))
                .sorted(comparator)
                .toList();
    }

    private Lote resolveOrCreateLote(Producto producto, StockMovimientoRequest request) {
        if (request.loteId() != null) {
            return resolveRequiredExistingLote(producto, request.loteId(), "Lote no encontrado");
        }

        String codigoLote = trim(request.codigoLote());
        if (codigoLote == null) {
            throw new BusinessException("LOTE_REQUERIDO", "El producto maneja lotes. Debe indicar codigoLote o loteId");
        }
        if (producto.isManejaVencimiento() && request.fechaVencimiento() == null) {
            throw new BusinessException("LOTE_VENCIMIENTO_REQUERIDO", "El producto maneja vencimiento. Debe indicar fechaVencimiento");
        }

        return loteRepository.findByProductoIdAndCodigoLote(producto.getId(), codigoLote)
                .orElseGet(() -> {
                    Lote lote = new Lote();
                    lote.setProducto(producto);
                    lote.setCodigoLote(codigoLote);
                    lote.setFechaFabricacion(request.fechaFabricacion());
                    lote.setFechaIngreso(LocalDate.now());
                    lote.setFechaVencimiento(request.fechaVencimiento());
                    lote.setCostoUnitario(resolvePrecioCompra(request));
                    lote.setEstado("ACTIVO");
                    return loteRepository.save(lote);
                });
    }

    private Lote resolveRequiredExistingLote(Producto producto, Long loteId, String message) {
        if (loteId == null) {
            throw new BusinessException("LOTE_REQUERIDO", message);
        }
        Lote lote = loteRepository.findById(loteId)
                .orElseThrow(() -> new BusinessException("LOTE_NO_ENCONTRADO", message));
        if (!lote.getProducto().getId().equals(producto.getId())) {
            throw new BusinessException("LOTE_PRODUCTO_INVALIDO", "El lote no pertenece al producto indicado");
        }
        return lote;
    }

    private Stock resolveStock(Producto producto, Almacen almacen) {
        return stockRepository.findByProductoIdAndAlmacenId(producto.getId(), almacen.getId())
                .orElseGet(() -> {
                    Stock newStock = new Stock();
                    newStock.setProducto(producto);
                    newStock.setAlmacen(almacen);
                    newStock.setCantidad(BigDecimal.ZERO);
                    newStock.setStockReservado(BigDecimal.ZERO);
                    newStock.setStockMinimo(producto.getStockMinimoGlobal() == null ? BigDecimal.ZERO : producto.getStockMinimoGlobal());
                    newStock.setEstado("ACTIVO");
                    return newStock;
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

    private KardexMovimiento saveMovimiento(
            Producto producto,
            Almacen almacen,
            Lote lote,
            String tipoMovimiento,
            StockMovimientoRequest request,
            BigDecimal cantidad,
            BigDecimal stockAnterior,
            BigDecimal stockNuevo
    ) {
        BigDecimal costoUnitario = resolvePrecioCompra(request);
        BigDecimal precioVenta = request.precioVenta() == null ? producto.getPrecioVentaBase() : request.precioVenta();

        KardexMovimiento movimiento = new KardexMovimiento();
        movimiento.setProducto(producto);
        movimiento.setAlmacen(almacen);
        movimiento.setLote(lote);
        movimiento.setTipoMovimiento(tipoMovimiento);
        movimiento.setMotivo(defaultIfBlank(request.motivo(), "SIN_MOTIVO"));
        movimiento.setCantidad(cantidad);
        movimiento.setStockAnterior(stockAnterior);
        movimiento.setStockNuevo(stockNuevo);
        movimiento.setSaldoResultante(stockNuevo);
        movimiento.setCostoUnitario(costoUnitario);
        movimiento.setCostoTotal(costoUnitario.multiply(cantidad));
        movimiento.setPrecioCompra(costoUnitario);
        movimiento.setPrecioVenta(precioVenta);
        movimiento.setUsuarioId(trim(request.usuarioId()));
        movimiento.setReferencia(trim(request.referencia()));
        movimiento.setReferenciaTipo(resolveReferenciaTipo(request.referencia()));
        movimiento.setFechaMovimiento(OffsetDateTime.now());
        movimiento.setObservacion(trim(request.referencia()));
        return kardexRepository.save(movimiento);
    }

    private void actualizarCostosProductoPorEntrada(
            Producto producto,
            StockMovimientoRequest request,
            BigDecimal cantidad,
            BigDecimal saldoAnterior,
            BigDecimal nuevoSaldo
    ) {
        BigDecimal precioCompra = request.precioCompra();
        if (precioCompra != null && nuevoSaldo.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal costoAnterior = producto.getCostoPromedio() == null ? BigDecimal.ZERO : producto.getCostoPromedio();
            BigDecimal costoPonderado = costoAnterior.multiply(saldoAnterior).add(precioCompra.multiply(cantidad)).divide(nuevoSaldo, 6, java.math.RoundingMode.HALF_UP);
            producto.setCostoPromedio(costoPonderado);
            producto.setPrecioCompraBase(precioCompra);
        }
        if (request.precioVenta() != null) {
            producto.setPrecioVentaBase(request.precioVenta());
            producto.setPrecio(request.precioVenta());
        }
        if (precioCompra != null || request.precioVenta() != null) {
            productoRepository.save(producto);
        }
    }

    private BigDecimal resolvePrecioCompra(StockMovimientoRequest request) {
        if (request.precioCompra() != null) {
            return request.precioCompra();
        }
        return request.costoUnitario() == null ? BigDecimal.ZERO : request.costoUnitario();
    }

    private void validateProductoMovible(Producto producto) {
        if (!producto.isActivo() || !"ACTIVO".equalsIgnoreCase(producto.getEstado())) {
            throw new BusinessException("PRODUCTO_INACTIVO", "No se puede mover stock de un producto inactivo");
        }
        if (!producto.isManejaStock()) {
            throw new BusinessException("PRODUCTO_SIN_STOCK", "El producto esta configurado como servicio/sin stock");
        }
    }

    private void validateAlmacenOperativo(Almacen almacen) {
        if (!almacen.isActivo() || !"ACTIVO".equalsIgnoreCase(almacen.getEstado())) {
            throw new BusinessException("ALMACEN_INACTIVO", "No se puede mover stock en un almacen inactivo");
        }
        if (almacen.getSucursal() == null || !almacen.getSucursal().isActivo()) {
            throw new BusinessException("SUCURSAL_INACTIVA", "La sucursal del almacen esta inactiva y no permite movimientos");
        }
    }

    private void validateLoteVigente(Lote lote) {
        LocalDate vencimiento = lote.getFechaVencimiento();
        if (vencimiento != null && vencimiento.isBefore(LocalDate.now())) {
            throw new BusinessException("LOTE_VENCIDO", "No se permite vender o trasladar lote vencido: " + lote.getCodigoLote());
        }
    }

    private String normalizeTipo(String raw) {
        return defaultIfBlank(raw, "").toUpperCase();
    }

    private BigDecimal normalizeCantidad(BigDecimal cantidad) {
        BigDecimal value = cantidad == null ? BigDecimal.ZERO : cantidad;
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("CANTIDAD_INVALIDA", "La cantidad debe ser mayor a cero");
        }
        return value;
    }

    private String resolveReferenciaTipo(String referencia) {
        String value = trim(referencia);
        if (value == null) {
            return null;
        }
        int separator = value.indexOf(':');
        return separator > 0 ? value.substring(0, separator).toUpperCase() : null;
    }

    private KardexMovimientoResponse toKardexResponse(KardexMovimiento saved) {
        return new KardexMovimientoResponse(
                saved.getId(),
                saved.getProducto().getId(),
                saved.getProducto().getSku(),
                saved.getProducto().getNombre(),
                saved.getAlmacen().getId(),
                saved.getAlmacen().getCodigo(),
                saved.getTipoMovimiento(),
                saved.getMotivo(),
                saved.getCantidad(),
                saved.getSaldoResultante(),
                saved.getReferencia(),
                saved.getFechaMovimiento()
        );
    }

    public StockResponse toStockResponse(Stock stock) {
        BigDecimal stockMinimo = resolveStockMinimo(stock);
        BigDecimal cantidad = stock.getCantidad() == null ? BigDecimal.ZERO : stock.getCantidad();
        boolean controlaStock = stock.getProducto().isActivo() && stock.getProducto().isManejaStock();
        return new StockResponse(
                stock.getId(),
                stock.getProducto().getId(),
                stock.getProducto().getSku(),
                stock.getProducto().getNombre(),
                stock.getAlmacen().getId(),
                stock.getAlmacen().getCodigo(),
                stock.getAlmacen().getNombre(),
                cantidad,
                stockMinimo,
                controlaStock && cantidad.compareTo(stockMinimo) <= 0,
                controlaStock && cantidad.compareTo(BigDecimal.ZERO) <= 0
        );
    }

    private BigDecimal resolveStockMinimo(Stock stock) {
        if (stock.getStockMinimo() != null && stock.getStockMinimo().compareTo(BigDecimal.ZERO) > 0) {
            return stock.getStockMinimo();
        }
        Producto producto = stock.getProducto();
        if (producto.getStockMinimoGlobal() != null && producto.getStockMinimoGlobal().compareTo(BigDecimal.ZERO) > 0) {
            return producto.getStockMinimoGlobal();
        }
        return producto.getStockMinimo() == null ? BigDecimal.ZERO : producto.getStockMinimo();
    }

    private String defaultIfBlank(String value, String defaultValue) {
        String trimmed = trim(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    private String trim(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }
}
