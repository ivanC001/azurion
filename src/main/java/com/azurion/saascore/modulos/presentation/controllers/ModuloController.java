package com.azurion.saascore.modulos.presentation.controllers;

import com.azurion.saascore.modulos.application.dto.CreateModuloRequest;
import com.azurion.saascore.modulos.application.dto.ModuloResponse;
import com.azurion.saascore.modulos.application.dto.UpdateModuloRequest;
import com.azurion.saascore.modulos.application.usecases.CreateModuloUseCase;
import com.azurion.saascore.modulos.application.usecases.GetModuloByIdUseCase;
import com.azurion.saascore.modulos.application.usecases.ListModulosUseCase;
import com.azurion.saascore.modulos.application.usecases.UpdateModuloUseCase;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/v1/saas/modulos", "/admin/modulos"})
@RequiredArgsConstructor
public class ModuloController {

    private final CreateModuloUseCase createModuloUseCase;
    private final ListModulosUseCase listModulosUseCase;
    private final GetModuloByIdUseCase getModuloByIdUseCase;
    private final UpdateModuloUseCase updateModuloUseCase;

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ADMIN_GENERAL')")
    public ApiResponse<ModuloResponse> create(@Valid @RequestBody CreateModuloRequest request) {
        return ApiResponse.ok(createModuloUseCase.execute(request), "Modulo creado");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ADMIN_GENERAL')")
    public ApiResponse<List<ModuloResponse>> list() {
        return ApiResponse.ok(listModulosUseCase.execute(), "Modulos");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ADMIN_GENERAL')")
    public ApiResponse<ModuloResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(getModuloByIdUseCase.execute(id), "Modulo");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ADMIN_GENERAL')")
    public ApiResponse<ModuloResponse> update(@PathVariable Long id,
                                              @Valid @RequestBody UpdateModuloRequest request) {
        return ApiResponse.ok(updateModuloUseCase.execute(id, request), "Modulo actualizado");
    }
}
