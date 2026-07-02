package com.azurion.saascore.inventory.presentation.controllers;

import com.azurion.saascore.inventory.application.dto.CompraResponse;
import com.azurion.saascore.inventory.application.dto.CreateCompraRequest;
import com.azurion.saascore.inventory.application.usecases.GetCompraByComprobanteUseCase;
import com.azurion.saascore.inventory.application.usecases.GetCompraUseCase;
import com.azurion.saascore.inventory.application.usecases.ListComprasUseCase;
import com.azurion.saascore.inventory.application.usecases.RegistrarCompraUseCase;
import com.azurion.saascore.modulos.application.services.RequireModule;
import com.azurion.shared.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/v1/saas/inventory/compras", "/inventario/compras"})
@RequiredArgsConstructor
@RequireModule({"INVENTARIO", "COMPRAS"})
public class InventoryCompraController {

    private final RegistrarCompraUseCase registrarCompraUseCase;
    private final ListComprasUseCase listComprasUseCase;
    private final GetCompraUseCase getCompraUseCase;
    private final GetCompraByComprobanteUseCase getCompraByComprobanteUseCase;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('COMPRAS_CREATE','INVENTORY_ENTRY')")
    public ApiResponse<CompraResponse> create(@Valid @RequestBody CreateCompraRequest request) {
        return ApiResponse.ok(registrarCompraUseCase.execute(request), "Compra registrada");
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('COMPRAS_READ','INVENTORY_READ')")
    public ApiResponse<List<CompraResponse>> list() {
        return ApiResponse.ok(listComprasUseCase.execute(), "Compras");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('COMPRAS_READ','INVENTORY_READ')")
    public ApiResponse<CompraResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(getCompraUseCase.execute(id), "Compra");
    }

    @GetMapping("/comprobante/{numeroComprobante}")
    @PreAuthorize("hasAnyAuthority('COMPRAS_READ','INVENTORY_READ')")
    public ApiResponse<CompraResponse> getByComprobante(@PathVariable String numeroComprobante) {
        return ApiResponse.ok(getCompraByComprobanteUseCase.execute(numeroComprobante), "Compra por comprobante");
    }
}
