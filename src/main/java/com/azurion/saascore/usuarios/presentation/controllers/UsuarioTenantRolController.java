package com.azurion.saascore.usuarios.presentation.controllers;

import com.azurion.saascore.usuarios.application.dto.AssignUsuarioTenantRolRequest;
import com.azurion.saascore.usuarios.application.dto.UsuarioTenantRolResponse;
import com.azurion.saascore.usuarios.application.services.TenantScopeValidator;
import com.azurion.saascore.usuarios.application.usecases.AssignUsuarioTenantRolUseCase;
import com.azurion.saascore.usuarios.application.usecases.ListUsuarioTenantRolesUseCase;
import com.azurion.saascore.usuarios.application.usecases.RemoveUsuarioTenantRolUseCase;
import com.azurion.shared.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/saas/usuarios-globales/{usuarioId}/tenants/{tenantId}/roles")
@RequiredArgsConstructor
public class UsuarioTenantRolController {

    private final AssignUsuarioTenantRolUseCase assignUsuarioTenantRolUseCase;
    private final ListUsuarioTenantRolesUseCase listUsuarioTenantRolesUseCase;
    private final RemoveUsuarioTenantRolUseCase removeUsuarioTenantRolUseCase;
    private final TenantScopeValidator tenantScopeValidator;

    @PostMapping
    @PreAuthorize("hasAuthority('USUARIOS_WRITE') or hasAnyRole('ADMIN_GENERAL','PLATFORM_ADMIN')")
    public ApiResponse<UsuarioTenantRolResponse> assign(@PathVariable Long usuarioId,
                                                        @PathVariable String tenantId,
                                                        @Valid @RequestBody AssignUsuarioTenantRolRequest request) {
        tenantScopeValidator.requireTenantId(tenantId);
        return ApiResponse.ok(
                assignUsuarioTenantRolUseCase.execute(usuarioId, tenantId, request.rolCodigo()),
                "Rol de tenant asignado"
        );
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USUARIOS_READ') or hasAnyRole('ADMIN_GENERAL','PLATFORM_ADMIN')")
    public ApiResponse<List<UsuarioTenantRolResponse>> list(@PathVariable Long usuarioId,
                                                            @PathVariable String tenantId) {
        tenantScopeValidator.requireTenantId(tenantId);
        return ApiResponse.ok(
                listUsuarioTenantRolesUseCase.execute(usuarioId, tenantId),
                "Roles de tenant del usuario"
        );
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('USUARIOS_WRITE') or hasAnyRole('ADMIN_GENERAL','PLATFORM_ADMIN')")
    public ApiResponse<String> remove(@PathVariable Long usuarioId,
                                      @PathVariable String tenantId,
                                      @RequestParam String rolCodigo) {
        tenantScopeValidator.requireTenantId(tenantId);
        removeUsuarioTenantRolUseCase.execute(usuarioId, tenantId, rolCodigo);
        return ApiResponse.ok("OK", "Rol de tenant removido");
    }
}
