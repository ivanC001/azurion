package com.azurion.saascore.inventory.presentation.controllers;

import com.azurion.saascore.inventory.application.dto.CreateProductoRequest;
import com.azurion.saascore.inventory.application.dto.ProductoResponse;
import com.azurion.saascore.inventory.application.dto.UpdateProductoRequest;
import com.azurion.saascore.inventory.application.usecases.CreateProductoUseCase;
import com.azurion.saascore.inventory.application.usecases.ListProductosUseCase;
import com.azurion.saascore.inventory.application.usecases.UpdateProductoUseCase;
import com.azurion.saascore.modulos.application.services.RequireModule;
import com.azurion.shared.api.ApiResponse;
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
    private final ListProductosUseCase listProductosUseCase;
    private final UpdateProductoUseCase updateProductoUseCase;

    @PostMapping
    @PreAuthorize("hasAuthority('PRODUCTOS_WRITE')")
    public ApiResponse<ProductoResponse> create(@Valid @RequestBody CreateProductoRequest request) {
        return ApiResponse.ok(createProductoUseCase.execute(request), "Producto creado");
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PRODUCTOS_READ')")
    public ApiResponse<List<ProductoResponse>> list(@RequestParam(required = false) Long almacenId) {
        return ApiResponse.ok(listProductosUseCase.execute(almacenId), "Productos");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCTOS_WRITE')")
    public ApiResponse<ProductoResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateProductoRequest request) {
        return ApiResponse.ok(updateProductoUseCase.execute(id, request), "Producto actualizado");
    }
}
