package com.azurion.saascore.tributacion.presentation.controllers;

import com.azurion.saascore.tributacion.application.dto.ConfiguracionTributariaRequest;
import com.azurion.saascore.tributacion.application.dto.ConfiguracionTributariaResponse;
import com.azurion.saascore.tributacion.application.dto.ProductoTributariaRequest;
import com.azurion.saascore.tributacion.application.dto.SucursalTributariaRequest;
import com.azurion.saascore.tributacion.application.dto.TaxResolution;
import com.azurion.saascore.tributacion.application.services.ConfiguracionTributariaService;
import com.azurion.shared.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/configuracion", "/v1/saas/configuracion"})
@RequiredArgsConstructor
public class ConfiguracionTributariaController {
    private final ConfiguracionTributariaService service;

    @GetMapping("/tributaria")
    @PreAuthorize("hasAuthority('TRIBUTACION_READ')")
    public ApiResponse<ConfiguracionTributariaResponse> getEmpresa() {
        return ApiResponse.ok(service.getEmpresa(), "Configuracion tributaria");
    }

    @PutMapping("/tributaria")
    @PreAuthorize("hasAuthority('TRIBUTACION_WRITE')")
    public ApiResponse<ConfiguracionTributariaResponse> updateEmpresa(@Valid @RequestBody ConfiguracionTributariaRequest request) {
        return ApiResponse.ok(service.updateEmpresa(request), "Configuracion tributaria actualizada");
    }

    @GetMapping("/sucursales/{id}/tributaria")
    @PreAuthorize("hasAuthority('TRIBUTACION_READ')")
    public ApiResponse<TaxResolution> getSucursal(@PathVariable Long id) {
        return ApiResponse.ok(service.getSucursal(id), "Configuracion tributaria de sucursal");
    }

    @PutMapping("/sucursales/{id}/tributaria")
    @PreAuthorize("hasAuthority('TRIBUTACION_WRITE')")
    public ApiResponse<TaxResolution> updateSucursal(@PathVariable Long id, @Valid @RequestBody SucursalTributariaRequest request) {
        return ApiResponse.ok(service.updateSucursal(id, request), "Configuracion tributaria de sucursal actualizada");
    }

    @GetMapping("/productos/{id}/tributaria")
    @PreAuthorize("hasAuthority('TRIBUTACION_READ')")
    public ApiResponse<TaxResolution> getProducto(@PathVariable Long id) {
        return ApiResponse.ok(service.getProducto(id), "Configuracion tributaria de producto");
    }

    @PutMapping("/productos/{id}/tributaria")
    @PreAuthorize("hasAuthority('TRIBUTACION_WRITE')")
    public ApiResponse<TaxResolution> updateProducto(@PathVariable Long id, @Valid @RequestBody ProductoTributariaRequest request) {
        return ApiResponse.ok(service.updateProducto(id, request), "Configuracion tributaria de producto actualizada");
    }
}
