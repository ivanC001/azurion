package com.azurion.saascore.usuarios.presentation.controllers;

import com.azurion.saascore.usuarios.application.dto.CreateUsuarioTenantRequest;
import com.azurion.saascore.usuarios.application.dto.SyncUsuarioRolesRequest;
import com.azurion.saascore.usuarios.application.dto.UpdateUsuarioPasswordRequest;
import com.azurion.saascore.usuarios.application.dto.UpdateUsuarioTenantRequest;
import com.azurion.saascore.usuarios.application.dto.UsuarioTenantResponse;
import com.azurion.saascore.usuarios.application.services.TenantScopeValidator;
import com.azurion.saascore.usuarios.application.usecases.CreateUsuarioTenantUseCase;
import com.azurion.saascore.usuarios.application.usecases.DeleteUsuarioTenantUseCase;
import com.azurion.saascore.usuarios.application.usecases.GetUsuarioTenantByIdUseCase;
import com.azurion.saascore.usuarios.application.usecases.ListUsuariosTenantUseCase;
import com.azurion.saascore.usuarios.application.usecases.SyncUsuarioTenantRolesUseCase;
import com.azurion.saascore.usuarios.application.usecases.UpdateUsuarioTenantPasswordUseCase;
import com.azurion.saascore.usuarios.application.usecases.UpdateUsuarioTenantUseCase;
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
@RequestMapping("/v1/saas/usuarios")
@RequiredArgsConstructor
public class UsuarioTenantController {

    private final CreateUsuarioTenantUseCase createUsuarioTenantUseCase;
    private final ListUsuariosTenantUseCase listUsuariosTenantUseCase;
    private final GetUsuarioTenantByIdUseCase getUsuarioTenantByIdUseCase;
    private final UpdateUsuarioTenantUseCase updateUsuarioTenantUseCase;
    private final UpdateUsuarioTenantPasswordUseCase updateUsuarioTenantPasswordUseCase;
    private final SyncUsuarioTenantRolesUseCase syncUsuarioTenantRolesUseCase;
    private final DeleteUsuarioTenantUseCase deleteUsuarioTenantUseCase;
    private final TenantScopeValidator tenantScopeValidator;

    @PostMapping
    @PreAuthorize("hasAuthority('USUARIOS_WRITE') or hasAnyRole('ADMIN_EMPRESA','ADMIN_GENERAL','PLATFORM_ADMIN')")
    public ApiResponse<UsuarioTenantResponse> create(@Valid @RequestBody CreateUsuarioTenantRequest request) {
        tenantScopeValidator.requireTenantContext();
        return ApiResponse.ok(createUsuarioTenantUseCase.execute(request), "Usuario creado");
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USUARIOS_READ') or hasAnyRole('ADMIN_EMPRESA','ADMIN_GENERAL','PLATFORM_ADMIN')")
    public ApiResponse<List<UsuarioTenantResponse>> list() {
        tenantScopeValidator.requireTenantContext();
        return ApiResponse.ok(listUsuariosTenantUseCase.execute(), "Usuarios");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USUARIOS_READ') or hasAnyRole('ADMIN_EMPRESA','ADMIN_GENERAL','PLATFORM_ADMIN')")
    public ApiResponse<UsuarioTenantResponse> getById(@PathVariable Long id) {
        tenantScopeValidator.requireTenantContext();
        return ApiResponse.ok(getUsuarioTenantByIdUseCase.execute(id), "Usuario");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USUARIOS_WRITE') or hasAnyRole('ADMIN_EMPRESA','ADMIN_GENERAL','PLATFORM_ADMIN')")
    public ApiResponse<UsuarioTenantResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody UpdateUsuarioTenantRequest request) {
        tenantScopeValidator.requireTenantContext();
        return ApiResponse.ok(updateUsuarioTenantUseCase.execute(id, request), "Usuario actualizado");
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasAnyRole('ADMIN_EMPRESA','ADMIN_GENERAL','PLATFORM_ADMIN')")
    public ApiResponse<String> updatePassword(@PathVariable Long id,
                                              @Valid @RequestBody UpdateUsuarioPasswordRequest request) {
        tenantScopeValidator.requireTenantContext();
        updateUsuarioTenantPasswordUseCase.execute(id, request);
        return ApiResponse.ok("OK", "Password actualizado");
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('USUARIOS_WRITE') or hasAnyRole('ADMIN_EMPRESA','ADMIN_GENERAL','PLATFORM_ADMIN')")
    public ApiResponse<UsuarioTenantResponse> syncRoles(@PathVariable Long id,
                                                        @Valid @RequestBody SyncUsuarioRolesRequest request) {
        tenantScopeValidator.requireTenantContext();
        return ApiResponse.ok(
                syncUsuarioTenantRolesUseCase.execute(id, request.rolCodigos()),
                "Roles de usuario actualizados"
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USUARIOS_WRITE') or hasAnyRole('ADMIN_EMPRESA','ADMIN_GENERAL','PLATFORM_ADMIN')")
    public ApiResponse<String> delete(@PathVariable Long id) {
        tenantScopeValidator.requireTenantContext();
        deleteUsuarioTenantUseCase.execute(id);
        return ApiResponse.ok("OK", "Usuario eliminado");
    }
}
