package com.azurion.saascore.crm.presentation.controllers;

import com.azurion.saascore.crm.application.dto.CrmLeadNotificationConfigResponse;
import com.azurion.saascore.crm.application.dto.CrmLeadNotificationDispatchResponse;
import com.azurion.saascore.crm.application.dto.CrmLeadNotificationTestResponse;
import com.azurion.saascore.crm.application.dto.UpdateCrmLeadNotificationConfigRequest;
import com.azurion.saascore.crm.application.services.CrmLeadNotificationConfigService;
import com.azurion.shared.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Avisos por correo cuando entra un lead o un mensaje nuevo al CRM.
 */
@RestController
@RequestMapping({"/v1/saas/crm", "/crm"})
@RequiredArgsConstructor
public class CrmLeadNotificationController {

    private final CrmLeadNotificationConfigService notificationConfigService;

    @GetMapping("/notificaciones/leads")
    @PreAuthorize("hasAuthority('CRM_CONFIG_MANAGE')")
    public ApiResponse<CrmLeadNotificationConfigResponse> getConfiguration() {
        return ApiResponse.ok(notificationConfigService.getConfiguration(), "Avisos de leads por correo");
    }

    @PutMapping("/notificaciones/leads")
    @PreAuthorize("hasAuthority('CRM_CONFIG_MANAGE')")
    public ApiResponse<CrmLeadNotificationConfigResponse> updateConfiguration(
            @Valid @RequestBody UpdateCrmLeadNotificationConfigRequest request) {
        return ApiResponse.ok(notificationConfigService.updateConfiguration(request), "Avisos de leads actualizados");
    }

    @GetMapping("/notificaciones/leads/historial")
    @PreAuthorize("hasAuthority('CRM_CONFIG_MANAGE')")
    public ApiResponse<List<CrmLeadNotificationDispatchResponse>> history() {
        return ApiResponse.ok(notificationConfigService.history(), "Historial de avisos enviados");
    }

    @PostMapping("/notificaciones/leads/prueba")
    @PreAuthorize("hasAuthority('CRM_CONFIG_MANAGE')")
    public ApiResponse<CrmLeadNotificationTestResponse> sendTest() {
        return ApiResponse.ok(notificationConfigService.sendTest(), "Prueba de avisos");
    }
}
