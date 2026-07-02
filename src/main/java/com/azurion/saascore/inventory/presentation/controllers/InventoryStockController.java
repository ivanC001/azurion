package com.azurion.saascore.inventory.presentation.controllers;

import com.azurion.saascore.inventory.application.dto.KardexMovimientoResponse;
import com.azurion.saascore.inventory.application.dto.LoteOrigenResponse;
import com.azurion.saascore.inventory.application.dto.LoteResponse;
import com.azurion.saascore.inventory.application.dto.StockMovimientoRequest;
import com.azurion.saascore.inventory.application.dto.StockLoteResponse;
import com.azurion.saascore.inventory.application.dto.StockResponse;
import com.azurion.saascore.inventory.application.usecases.GetLoteOrigenUseCase;
import com.azurion.saascore.inventory.application.usecases.ListKardexUseCase;
import com.azurion.saascore.inventory.application.usecases.ListLoteMovimientosUseCase;
import com.azurion.saascore.inventory.application.usecases.ListProductoLotesUseCase;
import com.azurion.saascore.inventory.application.usecases.ListStockLotesUseCase;
import com.azurion.saascore.inventory.application.usecases.ListStockUseCase;
import com.azurion.saascore.inventory.application.usecases.StockMovimientoUseCase;
import com.azurion.saascore.modulos.application.services.RequireModule;
import com.azurion.shared.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/v1/saas/inventory", "/inventario"})
@RequiredArgsConstructor
@RequireModule("INVENTARIO")
public class InventoryStockController {

    private final StockMovimientoUseCase stockMovimientoUseCase;
    private final ListStockUseCase listStockUseCase;
    private final ListStockLotesUseCase listStockLotesUseCase;
    private final ListKardexUseCase listKardexUseCase;
    private final ListProductoLotesUseCase listProductoLotesUseCase;
    private final GetLoteOrigenUseCase getLoteOrigenUseCase;
    private final ListLoteMovimientosUseCase listLoteMovimientosUseCase;

    @PostMapping("/movimientos")
    @PreAuthorize("hasAnyAuthority('INVENTORY_ENTRY','INVENTORY_EXIT','INVENTORY_ADJUST','INVENTORY_TRANSFER')")
    public ApiResponse<KardexMovimientoResponse> mover(@Valid @RequestBody StockMovimientoRequest request) {
        return ApiResponse.ok(stockMovimientoUseCase.execute(request), "Movimiento registrado");
    }

    @PostMapping("/entradas")
    @PreAuthorize("hasAuthority('INVENTORY_ENTRY')")
    public ApiResponse<KardexMovimientoResponse> entrada(@Valid @RequestBody StockMovimientoRequest request) {
        return ApiResponse.ok(stockMovimientoUseCase.execute(withTipo(request, "ENTRADA")), "Entrada registrada");
    }

    @PostMapping("/ajustes")
    @PreAuthorize("hasAuthority('INVENTORY_ADJUST')")
    public ApiResponse<KardexMovimientoResponse> ajuste(@Valid @RequestBody StockMovimientoRequest request) {
        return ApiResponse.ok(stockMovimientoUseCase.execute(withTipo(request, "AJUSTE")), "Ajuste registrado");
    }

    @GetMapping("/stock")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    public ApiResponse<List<StockResponse>> stock(
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) Long almacenId
    ) {
        return ApiResponse.ok(listStockUseCase.execute(productoId, almacenId), "Stock");
    }

    @GetMapping({"/stock/lotes", "/stock-lotes"})
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    public ApiResponse<List<StockLoteResponse>> stockLotes(
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) Long almacenId
    ) {
        return ApiResponse.ok(listStockLotesUseCase.execute(productoId, almacenId), "Stock por lote");
    }

    @GetMapping("/stock/almacen/{almacenId}")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    public ApiResponse<List<StockResponse>> stockPorAlmacen(@org.springframework.web.bind.annotation.PathVariable Long almacenId) {
        return ApiResponse.ok(listStockUseCase.execute(null, almacenId), "Stock por almacen");
    }

    @GetMapping("/almacenes/{almacenId}/stock")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    public ApiResponse<List<StockResponse>> stockAlmacen(@org.springframework.web.bind.annotation.PathVariable Long almacenId) {
        return ApiResponse.ok(listStockUseCase.execute(null, almacenId), "Stock por almacen");
    }

    @GetMapping("/sucursales/{sucursalId}/stock")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    public ApiResponse<List<StockResponse>> stockSucursal(@org.springframework.web.bind.annotation.PathVariable Long sucursalId) {
        return ApiResponse.ok(listStockUseCase.executeBySucursal(sucursalId), "Stock por sucursal");
    }

    @GetMapping("/stock/producto/{productoId}")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    public ApiResponse<List<StockResponse>> stockPorProducto(@org.springframework.web.bind.annotation.PathVariable Long productoId) {
        return ApiResponse.ok(listStockUseCase.execute(productoId, null), "Stock por producto");
    }

    @GetMapping("/productos/{productoId}/stock")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    public ApiResponse<List<StockResponse>> stockProducto(@org.springframework.web.bind.annotation.PathVariable Long productoId) {
        return ApiResponse.ok(listStockUseCase.execute(productoId, null), "Stock por producto");
    }

    @GetMapping("/productos/{productoId}/lotes")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    public ApiResponse<List<LoteResponse>> lotesPorProducto(@org.springframework.web.bind.annotation.PathVariable Long productoId) {
        return ApiResponse.ok(listProductoLotesUseCase.execute(productoId), "Lotes por producto");
    }

    @GetMapping("/kardex")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    public ApiResponse<List<KardexMovimientoResponse>> kardex(
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) Long almacenId
    ) {
        return ApiResponse.ok(listKardexUseCase.execute(productoId, almacenId), "Kardex");
    }

    @GetMapping("/kardex/producto/{productoId}")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    public ApiResponse<List<KardexMovimientoResponse>> kardexPorProducto(@org.springframework.web.bind.annotation.PathVariable Long productoId) {
        return ApiResponse.ok(listKardexUseCase.execute(productoId, null), "Kardex por producto");
    }

    @GetMapping("/kardex/almacen/{almacenId}")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    public ApiResponse<List<KardexMovimientoResponse>> kardexPorAlmacen(@org.springframework.web.bind.annotation.PathVariable Long almacenId) {
        return ApiResponse.ok(listKardexUseCase.execute(null, almacenId), "Kardex por almacen");
    }

    @GetMapping("/lotes/{loteId}/origen")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    public ApiResponse<LoteOrigenResponse> origenLote(@org.springframework.web.bind.annotation.PathVariable Long loteId) {
        return ApiResponse.ok(getLoteOrigenUseCase.execute(loteId), "Origen de lote");
    }

    @GetMapping("/lotes/{loteId}/movimientos")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    public ApiResponse<List<KardexMovimientoResponse>> movimientosLote(@org.springframework.web.bind.annotation.PathVariable Long loteId) {
        return ApiResponse.ok(listLoteMovimientosUseCase.execute(loteId), "Movimientos de lote");
    }

    private StockMovimientoRequest withTipo(StockMovimientoRequest request, String tipoMovimiento) {
        return new StockMovimientoRequest(
                request.productoId(),
                request.almacenId(),
                request.almacenDestinoId(),
                request.loteId(),
                request.codigoLote(),
                request.fechaFabricacion(),
                request.fechaVencimiento(),
                tipoMovimiento,
                request.motivo(),
                request.cantidad(),
                request.costoUnitario(),
                request.precioCompra(),
                request.precioVenta(),
                request.usuarioId(),
                request.referencia()
        );
    }
}
