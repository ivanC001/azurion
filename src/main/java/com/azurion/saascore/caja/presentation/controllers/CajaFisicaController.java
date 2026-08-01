package com.azurion.saascore.caja.presentation.controllers;

import com.azurion.saascore.caja.application.dto.CajaFisicaResponse;
import com.azurion.saascore.caja.application.dto.GuardarCajaFisicaRequest;
import com.azurion.saascore.caja.application.services.CajaFisicaService;
import com.azurion.saascore.modulos.application.services.RequireModule;
import com.azurion.shared.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/saas/cajas-fisicas")
@RequiredArgsConstructor
@RequireModule({"ERP", "CAJA"})
public class CajaFisicaController {

    private final CajaFisicaService cajaFisicaService;

    @GetMapping
    @PreAuthorize("hasAuthority('CAJA_READ')")
    public ApiResponse<List<CajaFisicaResponse>> list(
            @RequestParam(required = false) Long sucursalId) {
        return ApiResponse.ok(cajaFisicaService.list(sucursalId), "Cajas fisicas");
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('CAJA_CONFIGURE', 'CAJA_WRITE')")
    public ApiResponse<CajaFisicaResponse> create(
            @Valid @RequestBody GuardarCajaFisicaRequest request) {
        return ApiResponse.ok(cajaFisicaService.create(request), "Caja fisica creada");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CAJA_CONFIGURE', 'CAJA_WRITE')")
    public ApiResponse<CajaFisicaResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody GuardarCajaFisicaRequest request) {
        return ApiResponse.ok(cajaFisicaService.update(id, request), "Caja fisica actualizada");
    }
}
