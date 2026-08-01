package com.azurion.saascore.facturacion.presentation.controllers;

import com.azurion.saascore.facturacion.application.dto.CurrentFacturadorConfigurationResponse;
import com.azurion.saascore.facturacion.application.dto.FacturadorTenantConfigurationRequest;
import com.azurion.saascore.facturacion.application.services.FacturadorTenantPayloadFactory;
import com.azurion.saascore.facturacion.application.usecases.ManageCurrentEmpresaFacturadorUseCase;
import com.azurion.saascore.facturacion.infrastructure.http.FacturadorClient;
import com.azurion.saascore.modulos.application.services.RequireModule;
import com.azurion.shared.api.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/saas/facturador/tenants")
@RequiredArgsConstructor
public class FacturadorManagementController {

    private final FacturadorClient facturadorClient;
    private final FacturadorTenantPayloadFactory payloadFactory;
    private final ManageCurrentEmpresaFacturadorUseCase currentEmpresaUseCase;

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ADMIN_GENERAL')")
    public ApiResponse<JsonNode> index() {
        return ApiResponse.ok(facturadorClient.listarTenantsAdministrados(), "Tenants del facturador");
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ADMIN_GENERAL')")
    public ApiResponse<JsonNode> store(@ModelAttribute FacturadorTenantConfigurationRequest request) {
        return ApiResponse.ok(
                facturadorClient.crearTenantAdministrado(payloadFactory.create(request)),
                "Tenant del facturador creado"
        );
    }

    @GetMapping("/{tenantId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ADMIN_GENERAL')")
    public ApiResponse<JsonNode> show(@PathVariable long tenantId) {
        return ApiResponse.ok(facturadorClient.obtenerTenantAdministrado(tenantId), "Tenant del facturador");
    }

    @PutMapping(value = "/{tenantId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ADMIN_GENERAL')")
    public ApiResponse<JsonNode> update(
            @PathVariable long tenantId,
            @ModelAttribute FacturadorTenantConfigurationRequest request
    ) {
        return ApiResponse.ok(
                facturadorClient.actualizarTenantAdministrado(tenantId, payloadFactory.create(request)),
                "Tenant del facturador actualizado"
        );
    }

    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('ADMIN_EMPRESA','PLATFORM_ADMIN','ADMIN_GENERAL')")
    @RequireModule("ERP")
    public ApiResponse<JsonNode> current() {
        return ApiResponse.ok(currentEmpresaUseCase.getConfiguration(), "Configuracion del facturador");
    }

    @PutMapping(value = "/current", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN_EMPRESA','PLATFORM_ADMIN','ADMIN_GENERAL')")
    @RequireModule("ERP")
    public ApiResponse<CurrentFacturadorConfigurationResponse> updateCurrent(
            @ModelAttribute FacturadorTenantConfigurationRequest request
    ) {
        return ApiResponse.ok(
                currentEmpresaUseCase.updateConfiguration(request),
                "Configuracion del facturador actualizada"
        );
    }
}
