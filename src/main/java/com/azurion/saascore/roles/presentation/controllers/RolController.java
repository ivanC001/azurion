package com.azurion.saascore.roles.presentation.controllers;

import com.azurion.saascore.roles.application.dto.CreateRolRequest;
import com.azurion.saascore.roles.application.dto.PermisoResponse;
import com.azurion.saascore.roles.application.dto.RolResponse;
import com.azurion.saascore.roles.application.dto.SyncRolPermisosRequest;
import com.azurion.saascore.roles.application.dto.UpdateRolRequest;
import com.azurion.saascore.roles.application.usecases.AddPermisoToRolUseCase;
import com.azurion.saascore.roles.application.usecases.CreateRolUseCase;
import com.azurion.saascore.roles.application.usecases.DeleteRolUseCase;
import com.azurion.saascore.roles.application.usecases.GetRolByIdUseCase;
import com.azurion.saascore.roles.application.usecases.ListPermisosByRolUseCase;
import com.azurion.saascore.roles.application.usecases.ListRolesUseCase;
import com.azurion.saascore.roles.application.usecases.RemovePermisoFromRolUseCase;
import com.azurion.saascore.roles.application.usecases.SyncRolPermisosUseCase;
import com.azurion.saascore.roles.application.usecases.UpdateRolUseCase;
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
@RequestMapping("/v1/saas/roles")
@RequiredArgsConstructor
public class RolController {

    private final CreateRolUseCase createRolUseCase;
    private final ListRolesUseCase listRolesUseCase;
    private final GetRolByIdUseCase getRolByIdUseCase;
    private final UpdateRolUseCase updateRolUseCase;
    private final DeleteRolUseCase deleteRolUseCase;
    private final ListPermisosByRolUseCase listPermisosByRolUseCase;
    private final AddPermisoToRolUseCase addPermisoToRolUseCase;
    private final RemovePermisoFromRolUseCase removePermisoFromRolUseCase;
    private final SyncRolPermisosUseCase syncRolPermisosUseCase;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLES_WRITE') or hasAnyRole('ADMIN_GENERAL','PLATFORM_ADMIN')")
    public ApiResponse<RolResponse> create(@Valid @RequestBody CreateRolRequest request) {
        return ApiResponse.ok(createRolUseCase.execute(request), "Rol creado");
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLES_READ') or hasAnyRole('ADMIN_GENERAL','PLATFORM_ADMIN')")
    public ApiResponse<List<RolResponse>> list() {
        return ApiResponse.ok(listRolesUseCase.execute(), "Roles");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLES_READ') or hasAnyRole('ADMIN_GENERAL','PLATFORM_ADMIN')")
    public ApiResponse<RolResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(getRolByIdUseCase.execute(id), "Rol");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLES_WRITE') or hasAnyRole('ADMIN_GENERAL','PLATFORM_ADMIN')")
    public ApiResponse<RolResponse> update(@PathVariable Long id,
                                           @Valid @RequestBody UpdateRolRequest request) {
        return ApiResponse.ok(updateRolUseCase.execute(id, request), "Rol actualizado");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLES_WRITE') or hasAnyRole('ADMIN_GENERAL','PLATFORM_ADMIN')")
    public ApiResponse<String> delete(@PathVariable Long id) {
        deleteRolUseCase.execute(id);
        return ApiResponse.ok("OK", "Rol eliminado");
    }

    @GetMapping("/{id}/permisos")
    @PreAuthorize("hasAuthority('ROLES_READ') or hasAnyRole('ADMIN_GENERAL','PLATFORM_ADMIN')")
    public ApiResponse<List<PermisoResponse>> listPermisos(@PathVariable Long id) {
        return ApiResponse.ok(listPermisosByRolUseCase.execute(id), "Permisos del rol");
    }

    @PostMapping("/{id}/permisos/{permisoId}")
    @PreAuthorize("hasAuthority('ROLES_WRITE') or hasAnyRole('ADMIN_GENERAL','PLATFORM_ADMIN')")
    public ApiResponse<RolResponse> addPermiso(@PathVariable Long id,
                                               @PathVariable Long permisoId) {
        return ApiResponse.ok(addPermisoToRolUseCase.execute(id, permisoId), "Permiso agregado al rol");
    }

    @DeleteMapping("/{id}/permisos/{permisoId}")
    @PreAuthorize("hasAuthority('ROLES_WRITE') or hasAnyRole('ADMIN_GENERAL','PLATFORM_ADMIN')")
    public ApiResponse<RolResponse> removePermiso(@PathVariable Long id,
                                                  @PathVariable Long permisoId) {
        return ApiResponse.ok(removePermisoFromRolUseCase.execute(id, permisoId), "Permiso removido del rol");
    }

    @PutMapping("/{id}/permisos")
    @PreAuthorize("hasAuthority('ROLES_WRITE') or hasAnyRole('ADMIN_GENERAL','PLATFORM_ADMIN')")
    public ApiResponse<RolResponse> syncPermisos(@PathVariable Long id,
                                                 @Valid @RequestBody SyncRolPermisosRequest request) {
        return ApiResponse.ok(syncRolPermisosUseCase.execute(id, request.permisoIds()), "Permisos del rol actualizados");
    }
}
