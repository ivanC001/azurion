package com.azurion.saascore.suscripciones.presentation.controllers;

import com.azurion.saascore.suscripciones.application.dto.CreateSuscripcionRequest;
import com.azurion.saascore.suscripciones.application.dto.SuscripcionResponse;
import com.azurion.saascore.suscripciones.application.dto.UpdateSuscripcionEstadoRequest;
import com.azurion.saascore.suscripciones.application.usecases.CreateSuscripcionUseCase;
import com.azurion.saascore.suscripciones.application.usecases.GetSuscripcionByIdUseCase;
import com.azurion.saascore.suscripciones.application.usecases.ListSuscripcionesUseCase;
import com.azurion.saascore.suscripciones.application.usecases.UpdateSuscripcionEstadoUseCase;
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
@RequestMapping("/v1/saas/suscripciones")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ADMIN_GENERAL')")
public class SuscripcionController {

    private final CreateSuscripcionUseCase createSuscripcionUseCase;
    private final ListSuscripcionesUseCase listSuscripcionesUseCase;
    private final GetSuscripcionByIdUseCase getSuscripcionByIdUseCase;
    private final UpdateSuscripcionEstadoUseCase updateSuscripcionEstadoUseCase;

    @PostMapping
    public ApiResponse<SuscripcionResponse> create(@Valid @RequestBody CreateSuscripcionRequest request) {
        return ApiResponse.ok(createSuscripcionUseCase.execute(request), "Suscripcion creada");
    }

    @GetMapping
    public ApiResponse<List<SuscripcionResponse>> list(@RequestParam(required = false) Long empresaId) {
        return ApiResponse.ok(listSuscripcionesUseCase.execute(empresaId), "Suscripciones");
    }

    @GetMapping("/{id}")
    public ApiResponse<SuscripcionResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(getSuscripcionByIdUseCase.execute(id), "Suscripcion");
    }

    @PutMapping("/{id}/estado")
    public ApiResponse<SuscripcionResponse> updateEstado(@PathVariable Long id,
                                                         @Valid @RequestBody UpdateSuscripcionEstadoRequest request) {
        return ApiResponse.ok(updateSuscripcionEstadoUseCase.execute(id, request), "Suscripcion actualizada");
    }
}
