package com.azurion.saascore.sucursales.presentation.controllers;

import com.azurion.saascore.sucursales.application.dto.CreateSucursalRequest;
import com.azurion.saascore.sucursales.application.dto.ChangeSucursalEstadoRequest;
import com.azurion.saascore.sucursales.application.dto.SucursalResponse;
import com.azurion.saascore.sucursales.application.dto.UpdateSucursalRequest;
import com.azurion.saascore.sucursales.application.usecases.ChangeSucursalEstadoUseCase;
import com.azurion.saascore.sucursales.application.usecases.CreateSucursalUseCase;
import com.azurion.saascore.sucursales.application.usecases.ListSucursalesUseCase;
import com.azurion.saascore.sucursales.application.usecases.UpdateSucursalUseCase;
import com.azurion.shared.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/saas/sucursales")
@RequiredArgsConstructor
public class SucursalController {

    private final CreateSucursalUseCase createSucursalUseCase;
    private final ListSucursalesUseCase listSucursalesUseCase;
    private final UpdateSucursalUseCase updateSucursalUseCase;
    private final ChangeSucursalEstadoUseCase changeSucursalEstadoUseCase;

    @PostMapping
    @PreAuthorize("hasAuthority('SUCURSALES_WRITE')")
    public ApiResponse<SucursalResponse> create(@Valid @RequestBody CreateSucursalRequest request) {
        return ApiResponse.ok(createSucursalUseCase.execute(request), "Sucursal creada");
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SUCURSALES_READ') or hasAnyRole('ADMIN_GENERAL','PLATFORM_ADMIN')")
    public ApiResponse<List<SucursalResponse>> list() {
        return ApiResponse.ok(listSucursalesUseCase.execute(), "Sucursales");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SUCURSALES_WRITE')")
    public ApiResponse<SucursalResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSucursalRequest request
    ) {
        return ApiResponse.ok(updateSucursalUseCase.execute(id, request), "Sucursal actualizada");
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('SUCURSALES_WRITE')")
    public ApiResponse<SucursalResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeSucursalEstadoRequest request
    ) {
        return ApiResponse.ok(
                changeSucursalEstadoUseCase.execute(id, request.activo()),
                request.activo() ? "Sucursal habilitada" : "Sucursal deshabilitada"
        );
    }
}
