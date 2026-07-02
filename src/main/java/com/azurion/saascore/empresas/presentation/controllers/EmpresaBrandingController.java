package com.azurion.saascore.empresas.presentation.controllers;

import com.azurion.saascore.empresas.application.dto.EmpresaResponse;
import com.azurion.saascore.empresas.application.dto.UpdateEmpresaBrandingRequest;
import com.azurion.saascore.empresas.application.usecases.GetCurrentEmpresaUseCase;
import com.azurion.saascore.empresas.application.usecases.UpdateCurrentEmpresaBrandingUseCase;
import com.azurion.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/saas/empresas/current")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN_EMPRESA','ADMIN','PLATFORM_ADMIN','ADMIN_GENERAL')")
public class EmpresaBrandingController {

    private final GetCurrentEmpresaUseCase getCurrentEmpresaUseCase;
    private final UpdateCurrentEmpresaBrandingUseCase updateCurrentEmpresaBrandingUseCase;

    @GetMapping
    public ApiResponse<EmpresaResponse> getCurrent() {
        return ApiResponse.ok(getCurrentEmpresaUseCase.execute(), "Empresa actual");
    }

    @PutMapping(value = "/branding", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<EmpresaResponse> updateBranding(@ModelAttribute UpdateEmpresaBrandingRequest request) {
        return ApiResponse.ok(updateCurrentEmpresaBrandingUseCase.execute(request), "Branding de empresa actualizado");
    }
}
