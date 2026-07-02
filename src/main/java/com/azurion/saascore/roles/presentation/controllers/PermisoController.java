package com.azurion.saascore.roles.presentation.controllers;

import com.azurion.saascore.roles.application.dto.CreatePermisoRequest;
import com.azurion.saascore.roles.application.dto.PermisoResponse;
import com.azurion.saascore.roles.application.dto.UpdatePermisoRequest;
import com.azurion.saascore.roles.application.usecases.CreatePermisoUseCase;
import com.azurion.saascore.roles.application.usecases.DeletePermisoUseCase;
import com.azurion.saascore.roles.application.usecases.GetPermisoByIdUseCase;
import com.azurion.saascore.roles.application.usecases.ListPermisosUseCase;
import com.azurion.saascore.roles.application.usecases.UpdatePermisoUseCase;
import com.azurion.shared.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/saas/permisos")
@RequiredArgsConstructor
public class PermisoController {

    private final CreatePermisoUseCase createPermisoUseCase;
    private final ListPermisosUseCase listPermisosUseCase;
    private final GetPermisoByIdUseCase getPermisoByIdUseCase;
    private final UpdatePermisoUseCase updatePermisoUseCase;
    private final DeletePermisoUseCase deletePermisoUseCase;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_GENERAL','PLATFORM_ADMIN')")
    public ApiResponse<PermisoResponse> create(@Valid @RequestBody CreatePermisoRequest request) {
        return ApiResponse.ok(createPermisoUseCase.execute(request), "Permiso creado");
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLES_READ') or hasAnyRole('ADMIN_GENERAL','PLATFORM_ADMIN')")
    public ApiResponse<List<PermisoResponse>> list() {
        return ApiResponse.ok(listPermisosUseCase.execute(), "Permisos");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLES_READ') or hasAnyRole('ADMIN_GENERAL','PLATFORM_ADMIN')")
    public ApiResponse<PermisoResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(getPermisoByIdUseCase.execute(id), "Permiso");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_GENERAL','PLATFORM_ADMIN')")
    public ApiResponse<PermisoResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody UpdatePermisoRequest request) {
        return ApiResponse.ok(updatePermisoUseCase.execute(id, request), "Permiso actualizado");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_GENERAL','PLATFORM_ADMIN')")
    public ApiResponse<String> delete(@PathVariable Long id) {
        deletePermisoUseCase.execute(id);
        return ApiResponse.ok("OK", "Permiso eliminado");
    }
}
