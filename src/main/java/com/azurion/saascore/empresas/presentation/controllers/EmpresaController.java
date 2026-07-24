package com.azurion.saascore.empresas.presentation.controllers;

import com.azurion.saascore.empresas.application.dto.CreateEmpresaRequest;
import com.azurion.saascore.empresas.application.dto.EmpresaOperationalSummaryResponse;
import com.azurion.saascore.empresas.application.dto.EmpresaResponse;
import com.azurion.saascore.empresas.application.usecases.CreateEmpresaUseCase;
import com.azurion.saascore.empresas.application.usecases.GetEmpresaByIdUseCase;
import com.azurion.saascore.empresas.application.usecases.ListEmpresaOperationalSummariesUseCase;
import com.azurion.saascore.empresas.application.usecases.ListEmpresasUseCase;
import com.azurion.shared.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/saas/empresas")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ADMIN_GENERAL')")
public class EmpresaController {

    private final CreateEmpresaUseCase createEmpresaUseCase;
    private final ListEmpresasUseCase listEmpresasUseCase;
    private final ListEmpresaOperationalSummariesUseCase listOperationalSummariesUseCase;
    private final GetEmpresaByIdUseCase getEmpresaByIdUseCase;

    @PostMapping
    public ApiResponse<EmpresaResponse> create(@Valid @RequestBody CreateEmpresaRequest request) {
        return ApiResponse.ok(createEmpresaUseCase.execute(request), "Empresa created");
    }

    @GetMapping
    public ApiResponse<List<EmpresaResponse>> list() {
        return ApiResponse.ok(listEmpresasUseCase.execute(), "Empresas");
    }

    @GetMapping("/resumen-operativo")
    public ApiResponse<List<EmpresaOperationalSummaryResponse>> operationalSummary() {
        return ApiResponse.ok(
                listOperationalSummariesUseCase.execute(),
                "Resumen operativo de empresas"
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<EmpresaResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(getEmpresaByIdUseCase.execute(id), "Empresa");
    }
}
