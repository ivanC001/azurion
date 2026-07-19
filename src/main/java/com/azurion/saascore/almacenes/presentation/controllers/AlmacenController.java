package com.azurion.saascore.almacenes.presentation.controllers;

import com.azurion.saascore.almacenes.application.dto.AlmacenResponse;
import com.azurion.saascore.almacenes.application.dto.CreateAlmacenRequest;
import com.azurion.saascore.almacenes.application.usecases.CreateAlmacenUseCase;
import com.azurion.saascore.almacenes.application.usecases.ListAlmacenesUseCase;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/v1/saas/almacenes", "/v1/saas/inventory/almacenes"})
@RequiredArgsConstructor
@RequireModule({"ERP", "INVENTARIO"})
public class AlmacenController {

    private final CreateAlmacenUseCase createAlmacenUseCase;
    private final ListAlmacenesUseCase listAlmacenesUseCase;

    @PostMapping
    @PreAuthorize("hasAuthority('INVENTORY_ADJUST')")
    public ApiResponse<AlmacenResponse> create(@Valid @RequestBody CreateAlmacenRequest request) {
        return ApiResponse.ok(createAlmacenUseCase.execute(request), "Almacen creado");
    }

    @GetMapping
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    public ApiResponse<List<AlmacenResponse>> list() {
        return ApiResponse.ok(listAlmacenesUseCase.execute(), "Almacenes");
    }
}
