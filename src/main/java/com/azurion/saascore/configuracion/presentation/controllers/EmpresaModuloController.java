package com.azurion.saascore.configuracion.presentation.controllers;

import com.azurion.saascore.configuracion.application.dto.AssignEmpresaModuloRequest;
import com.azurion.saascore.configuracion.application.dto.EmpresaModuloResponse;
import com.azurion.saascore.configuracion.application.dto.SyncEmpresaModulosRequest;
import com.azurion.saascore.configuracion.application.usecases.AsignarModulosEmpresaUseCase;
import com.azurion.saascore.configuracion.application.usecases.AssignEmpresaModuloUseCase;
import com.azurion.saascore.configuracion.application.usecases.ListEmpresaModulosUseCase;
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
@RequestMapping({"/v1/saas/empresas/{empresaId}/modulos", "/admin/empresas/{empresaId}/modulos"})
@RequiredArgsConstructor
public class EmpresaModuloController {

    private final ListEmpresaModulosUseCase listEmpresaModulosUseCase;
    private final AssignEmpresaModuloUseCase assignEmpresaModuloUseCase;
    private final AsignarModulosEmpresaUseCase asignarModulosEmpresaUseCase;

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ADMIN_GENERAL','ADMIN_EMPRESA','ADMIN') or hasAuthority('EMPRESA_MODULOS_READ')")
    public ApiResponse<List<EmpresaModuloResponse>> list(@PathVariable Long empresaId) {
        return ApiResponse.ok(listEmpresaModulosUseCase.execute(empresaId), "Modulos de empresa");
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ADMIN_GENERAL')")
    public ApiResponse<EmpresaModuloResponse> assign(@PathVariable Long empresaId,
                                                     @Valid @RequestBody AssignEmpresaModuloRequest request) {
        return ApiResponse.ok(assignEmpresaModuloUseCase.execute(empresaId, request), "Modulo asignado");
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ADMIN_GENERAL')")
    public ApiResponse<List<EmpresaModuloResponse>> sync(@PathVariable Long empresaId,
                                                         @Valid @RequestBody SyncEmpresaModulosRequest request) {
        return ApiResponse.ok(asignarModulosEmpresaUseCase.execute(empresaId, request), "Modulos de empresa actualizados");
    }
}
