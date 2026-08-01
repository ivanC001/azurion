package com.azurion.saascore.almacenes.presentation.controllers;

import com.azurion.saascore.almacenes.application.dto.AlmacenResponse;
import com.azurion.saascore.almacenes.application.dto.CreateAlmacenRequest;
import com.azurion.saascore.almacenes.application.dto.UpdateAlmacenRequest;
import com.azurion.saascore.almacenes.application.usecases.CreateAlmacenUseCase;
import com.azurion.saascore.almacenes.application.usecases.ListAlmacenesUseCase;
import com.azurion.saascore.almacenes.application.usecases.UpdateAlmacenUseCase;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping({"/v1/saas/almacenes", "/v1/saas/inventory/almacenes"})
@RequiredArgsConstructor
@RequireModule({"ERP", "INVENTARIO"})
public class AlmacenController {

    private final CreateAlmacenUseCase createAlmacenUseCase;
    private final ListAlmacenesUseCase listAlmacenesUseCase;
    private final UpdateAlmacenUseCase updateAlmacenUseCase;

    @PostMapping
    @PreAuthorize("hasAuthority('INVENTORY_ADJUST')")
    public ApiResponse<AlmacenResponse> create(@Valid @RequestBody CreateAlmacenRequest request) {
        return ApiResponse.ok(createAlmacenUseCase.execute(request), "Almacen creado");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_ADJUST')")
    public ApiResponse<AlmacenResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAlmacenRequest request
    ) {
        return ApiResponse.ok(updateAlmacenUseCase.execute(id, request), "Almacen actualizado");
    }

    @GetMapping
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    public ApiResponse<List<AlmacenResponse>> list() {
        return ApiResponse.ok(listAlmacenesUseCase.execute(), "Almacenes");
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    public ApiResponse<PageResponse<AlmacenResponse>> page(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(listAlmacenesUseCase.page(page, size), "Almacenes paginados");
    }
}
