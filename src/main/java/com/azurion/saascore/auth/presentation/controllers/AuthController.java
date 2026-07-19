package com.azurion.saascore.auth.presentation.controllers;

import com.azurion.saascore.auth.application.dto.LoginRequest;
import com.azurion.saascore.auth.application.dto.LoginResponse;
import com.azurion.saascore.auth.application.dto.RegisterAdminGeneralRequest;
import com.azurion.saascore.auth.application.dto.RegisterAdminGeneralResponse;
import com.azurion.saascore.auth.application.dto.TenantLoginResponse;
import com.azurion.saascore.auth.application.usecases.LoginUseCase;
import com.azurion.saascore.auth.application.usecases.RegisterAdminGeneralUseCase;
import com.azurion.shared.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RegisterAdminGeneralUseCase registerAdminGeneralUseCase;

    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ADMIN_GENERAL')")
    public ApiResponse<RegisterAdminGeneralResponse> register(@Valid @RequestBody RegisterAdminGeneralRequest request) {
        return ApiResponse.ok(registerAdminGeneralUseCase.execute(request), "Administrador general registrado");
    }

    @PostMapping("/public/login")
    public ApiResponse<LoginResponse> publicLogin(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(loginUseCase.executePublic(request), "Login public successful");
    }

    @PostMapping("/tenant/login")
    public ApiResponse<TenantLoginResponse> tenantLogin(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(loginUseCase.executeTenant(request), "Login tenant successful");
    }

}
