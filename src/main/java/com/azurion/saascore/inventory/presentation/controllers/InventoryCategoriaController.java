package com.azurion.saascore.inventory.presentation.controllers;

import com.azurion.saascore.inventory.application.dto.CategoriaResponse;
import com.azurion.saascore.inventory.application.usecases.ListCategoriasUseCase;
import com.azurion.shared.api.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/saas/inventory/categorias")
@RequiredArgsConstructor
public class InventoryCategoriaController {

    private final ListCategoriasUseCase listCategoriasUseCase;

    @GetMapping
    @PreAuthorize("hasAuthority('PRODUCTOS_READ')")
    public ApiResponse<List<CategoriaResponse>> list() {
        return ApiResponse.ok(listCategoriasUseCase.execute(), "Categorias de productos");
    }
}
