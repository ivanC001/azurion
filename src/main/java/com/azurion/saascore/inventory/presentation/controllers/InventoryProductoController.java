package com.azurion.saascore.inventory.presentation.controllers;

import com.azurion.saascore.inventory.application.dto.CreateProductoRequest;
import com.azurion.saascore.inventory.application.dto.CreateProductoRapidoRequest;
import com.azurion.saascore.inventory.application.dto.ProductoResponse;
import com.azurion.saascore.inventory.application.dto.UpdateProductoRequest;
import com.azurion.saascore.inventory.application.usecases.BuscarProductoPorCodigoUseCase;
import com.azurion.saascore.inventory.application.usecases.CreateProductoUseCase;
import com.azurion.saascore.inventory.application.usecases.CreateProductoRapidoUseCase;
import com.azurion.saascore.inventory.application.usecases.ListProductosUseCase;
import com.azurion.saascore.inventory.application.usecases.UpdateProductoUseCase;
import com.azurion.saascore.modulos.application.services.RequireModule;
import com.azurion.shared.api.ApiResponse;
import com.azurion.shared.api.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/v1/saas/inventory/productos", "/inventario/productos"})
@RequiredArgsConstructor
@RequireModule({"ERP", "INVENTARIO"})
public class InventoryProductoController {

    private final CreateProductoUseCase createProductoUseCase;
    private final CreateProductoRapidoUseCase createProductoRapidoUseCase;
    private final BuscarProductoPorCodigoUseCase buscarProductoPorCodigoUseCase;
    private final ListProductosUseCase listProductosUseCase;
    private final UpdateProductoUseCase updateProductoUseCase;

    @PostMapping
    @PreAuthorize("hasAuthority('PRODUCTOS_WRITE')")
    public ApiResponse<ProductoResponse> create(@Valid @RequestBody CreateProductoRequest request) {
        return ApiResponse.ok(createProductoUseCase.execute(request), "Producto creado");
    }

    @PostMapping("/rapido")
    @PreAuthorize("hasAuthority('PRODUCTOS_WRITE') and hasAuthority('INVENTORY_ENTRY')")
    public ApiResponse<ProductoResponse> createRapido(
            @Valid @RequestBody CreateProductoRapidoRequest request
    ) {
        return ApiResponse.ok(
                createProductoRapidoUseCase.execute(request),
                "Producto y existencias iniciales registrados"
        );
    }

    @GetMapping("/lookup")
    @PreAuthorize("hasAuthority('PRODUCTOS_READ')")
    public ApiResponse<ProductoResponse> lookup(@RequestParam String codigo) {
        return ApiResponse.ok(buscarProductoPorCodigoUseCase.execute(codigo), "Busqueda de producto");
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PRODUCTOS_READ')")
    public ApiResponse<List<ProductoResponse>> list(@RequestParam(required = false) Long almacenId) {
        return ApiResponse.ok(listProductosUseCase.execute(almacenId), "Productos");
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('PRODUCTOS_READ')")
    public ApiResponse<PageResponse<ProductoResponse>> page(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) Long almacenId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(listProductosUseCase.page(q, almacenId, page, size), "Productos paginados");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCTOS_WRITE')")
    public ApiResponse<ProductoResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateProductoRequest request) {
        return ApiResponse.ok(updateProductoUseCase.execute(id, request), "Producto actualizado");
    }
}
