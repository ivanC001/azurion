package com.azurion.saascore.settings.email.presentation.controllers;

import com.azurion.saascore.modulos.application.services.RequireModule;
import com.azurion.saascore.settings.email.application.dto.EmailConfigRequest;
import com.azurion.saascore.settings.email.application.dto.EmailConfigResponse;
import com.azurion.saascore.settings.email.application.dto.TestEmailRequest;
import com.azurion.saascore.settings.email.application.services.TenantEmailConfigService;
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
@RequestMapping({"/settings/email", "/v1/saas/settings/email"})
@RequiredArgsConstructor
@RequireModule("CRM")
public class TenantEmailConfigController {

    private final TenantEmailConfigService service;

    @GetMapping
    @PreAuthorize("hasAuthority('CRM_CONFIG_MANAGE')")
    public ApiResponse<EmailConfigResponse> getConfig() {
        return ApiResponse.ok(service.getCurrentTenantConfig(), "Configuracion de correo");
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CRM_CONFIG_MANAGE')")
    public ApiResponse<EmailConfigResponse> createOrUpdate(@Valid @RequestBody EmailConfigRequest request) {
        return ApiResponse.ok(service.saveOrUpdateConfig(request), "Configuracion de correo guardada");
    }

    @PutMapping
    @PreAuthorize("hasAuthority('CRM_CONFIG_MANAGE')")
    public ApiResponse<EmailConfigResponse> update(@Valid @RequestBody EmailConfigRequest request) {
        return ApiResponse.ok(service.saveOrUpdateConfig(request), "Configuracion de correo actualizada");
    }

    @PostMapping("/test")
    @PreAuthorize("hasAuthority('CRM_CONFIG_MANAGE')")
    public ApiResponse<EmailConfigResponse> test(@Valid @RequestBody TestEmailRequest request) {
        return ApiResponse.ok(service.testEmailConfig(request), "Correo de prueba enviado correctamente");
    }

    @PostMapping("/activate")
    @PreAuthorize("hasAuthority('CRM_CONFIG_MANAGE')")
    public ApiResponse<EmailConfigResponse> activate() {
        return ApiResponse.ok(service.activate(), "Configuracion de correo activada");
    }

    @PostMapping("/deactivate")
    @PreAuthorize("hasAuthority('CRM_CONFIG_MANAGE')")
    public ApiResponse<EmailConfigResponse> deactivate() {
        return ApiResponse.ok(service.deactivate(), "Configuracion de correo desactivada");
    }
}
