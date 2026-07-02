package com.azurion.saascore.modulos.presentation.controllers;

import com.azurion.saascore.modulos.application.dto.ActiveModulesResponse;
import com.azurion.saascore.modulos.application.usecases.ObtenerModulosActivosEmpresaUseCase;
import com.azurion.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/v1/me/modules", "/me/modules"})
@RequiredArgsConstructor
public class MeModuleController {

    private final ObtenerModulosActivosEmpresaUseCase obtenerModulosActivosEmpresaUseCase;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ActiveModulesResponse> listCurrentTenantModules() {
        return ApiResponse.ok(obtenerModulosActivosEmpresaUseCase.executeCurrentTenant(), "Modulos activos");
    }
}
