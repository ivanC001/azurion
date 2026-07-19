package com.azurion.saascore.facturacion.presentation.controllers;

import com.azurion.saascore.facturacion.application.dto.GuiaRemisionResponse;
import com.azurion.saascore.facturacion.application.dto.RegistrarGuiaRemisionRequest;
import com.azurion.saascore.facturacion.application.dto.RegistrarGuiaRemisionResponse;
import com.azurion.saascore.facturacion.application.usecases.ListGuiasRemisionUseCase;
import com.azurion.saascore.facturacion.application.usecases.RegistrarGuiaRemisionUseCase;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/saas/guias")
@RequiredArgsConstructor
@RequireModule({"ERP", "FACTURACION"})
public class GuiaRemisionController {

    private final RegistrarGuiaRemisionUseCase registrarGuiaRemisionUseCase;
    private final ListGuiasRemisionUseCase listGuiasRemisionUseCase;

    @GetMapping("/remision")
    @PreAuthorize("hasAuthority('FACTURACION_READ')")
    public ApiResponse<List<GuiaRemisionResponse>> listar(@RequestParam(required = false) String q) {
        return ApiResponse.ok(listGuiasRemisionUseCase.execute(q), "Guias de remision");
    }

    @PostMapping("/remision")
    @PreAuthorize("hasAuthority('GUIA_REMISION_CREATE')")
    public ApiResponse<RegistrarGuiaRemisionResponse> registrar(@Valid @RequestBody RegistrarGuiaRemisionRequest request) {
        return ApiResponse.ok(registrarGuiaRemisionUseCase.execute(request), "Guia de remision enviada al facturador");
    }
}
