package com.azurion.saascore.auth.presentation.controllers;

import com.azurion.saascore.auth.application.dto.LoginRequest;
import com.azurion.saascore.auth.application.dto.LoginResponse;
import com.azurion.saascore.auth.application.dto.RegisterAdminGeneralRequest;
import com.azurion.saascore.auth.application.dto.RegisterAdminGeneralResponse;
import com.azurion.saascore.auth.application.dto.ReplaceSessionRequest;
import com.azurion.saascore.auth.application.dto.TenantLoginResponse;
import com.azurion.saascore.auth.application.usecases.LoginUseCase;
import com.azurion.saascore.auth.application.usecases.RegisterAdminGeneralUseCase;
import com.azurion.shared.api.ApiResponse;
import com.azurion.security.jwt.TenantAuthenticationDetails;
import com.azurion.security.session.AuthSessionService;
import com.azurion.security.session.SessionClientInfo;
import com.azurion.shared.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RegisterAdminGeneralUseCase registerAdminGeneralUseCase;
    private final AuthSessionService authSessionService;

    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ADMIN_GENERAL')")
    public ApiResponse<RegisterAdminGeneralResponse> register(@Valid @RequestBody RegisterAdminGeneralRequest request) {
        return ApiResponse.ok(registerAdminGeneralUseCase.execute(request), "Administrador general registrado");
    }

    @PostMapping("/public/login")
    public ApiResponse<LoginResponse> publicLogin(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.ok(
                loginUseCase.executePublic(
                        request,
                        SessionClientInfo.from(
                                httpRequest,
                                request.deviceId(),
                                request.deviceName()
                        )
                ),
                "Login public successful"
        );
    }

    @PostMapping("/tenant/login")
    public ApiResponse<TenantLoginResponse> tenantLogin(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.ok(
                loginUseCase.executeTenant(
                        request,
                        SessionClientInfo.from(
                                httpRequest,
                                request.deviceId(),
                                request.deviceName()
                        )
                ),
                "Login tenant successful"
        );
    }

    @PostMapping("/session/replace")
    public ApiResponse<Object> replaceSession(@Valid @RequestBody ReplaceSessionRequest request) {
        return ApiResponse.ok(
                loginUseCase.replaceSession(request.replacementToken(), request.deviceId()),
                "Sesion reemplazada correctamente"
        );
    }

    @PostMapping("/session/logout")
    public ApiResponse<Void> logout(
            Authentication authentication,
            HttpServletRequest request
    ) {
        if (!(authentication.getDetails() instanceof TenantAuthenticationDetails details)) {
            throw new BusinessException("AUTH_SESSION_INVALID", "No se pudo identificar la sesion");
        }
        authSessionService.logout(
                details.getSessionTenantId(),
                details.getUserId(),
                details.getSessionId(),
                SessionClientInfo.from(request, "unknown", "Navegador actual")
        );
        return ApiResponse.ok(null, "Sesion cerrada correctamente");
    }

}
