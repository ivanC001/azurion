package com.azurion.saascore.messaging.presentation.controllers;

import com.azurion.saascore.messaging.application.dto.SendPlatformMessageRequest;
import com.azurion.saascore.messaging.application.dto.SentPlatformMessageResponse;
import com.azurion.saascore.messaging.application.services.PlatformMessagingService;
import com.azurion.shared.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/saas/platform/messages")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ADMIN_GENERAL')")
public class PlatformMessageAdminController {

    private final PlatformMessagingService service;

    @PostMapping
    public ApiResponse<SentPlatformMessageResponse> send(
            @Valid @RequestBody SendPlatformMessageRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.send(request, authentication), "Mensaje enviado a las bandejas");
    }

    @GetMapping
    public ApiResponse<List<SentPlatformMessageResponse>> sent(
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ApiResponse.ok(service.sentMessages(limit), "Mensajes enviados");
    }
}
