package com.azurion.saascore.settings.email.presentation.controllers;

import com.azurion.saascore.settings.email.application.dto.PlatformEmailConfigRequest;
import com.azurion.saascore.settings.email.application.dto.PlatformEmailConfigResponse;
import com.azurion.saascore.settings.email.application.dto.TestEmailRequest;
import com.azurion.saascore.settings.email.application.services.PlatformEmailConfigService;
import com.azurion.shared.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/saas/platform/email")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ADMIN_GENERAL')")
public class PlatformEmailConfigController {

    private final PlatformEmailConfigService service;

    @GetMapping
    public ApiResponse<PlatformEmailConfigResponse> getConfig() {
        return ApiResponse.ok(service.getConfig(), "Configuracion de correo de Azurion");
    }

    @PutMapping
    public ApiResponse<PlatformEmailConfigResponse> save(@Valid @RequestBody PlatformEmailConfigRequest request) {
        return ApiResponse.ok(service.saveOrUpdate(request), "Configuracion de correo de Azurion guardada");
    }

    @PostMapping("/test")
    public ApiResponse<PlatformEmailConfigResponse> test(@Valid @RequestBody TestEmailRequest request) {
        return ApiResponse.ok(service.test(request), "Correo de prueba enviado correctamente");
    }

    @PostMapping("/activate")
    public ApiResponse<PlatformEmailConfigResponse> activate() {
        return ApiResponse.ok(service.activate(), "Correo global de Azurion activado");
    }

    @PostMapping("/deactivate")
    public ApiResponse<PlatformEmailConfigResponse> deactivate() {
        return ApiResponse.ok(service.deactivate(), "Correo global de Azurion desactivado");
    }
}
