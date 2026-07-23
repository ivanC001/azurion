package com.azurion.saascore.crm.presentation.controllers;

import com.azurion.saascore.crm.application.dto.AssignWhatsappConversationRequest;
import com.azurion.saascore.crm.application.dto.CrmWhatsappConversationResponse;
import com.azurion.saascore.crm.application.dto.CrmWhatsappMessageResponse;
import com.azurion.saascore.crm.application.dto.SendWhatsappMessageRequest;
import com.azurion.saascore.crm.application.dto.UpdateWhatsappConversationNoteRequest;
import com.azurion.saascore.crm.application.dto.UpdateWhatsappConversationStatusRequest;
import com.azurion.saascore.crm.application.dto.WhatsappConnectionStatusResponse;
import com.azurion.saascore.crm.application.dto.WhatsappVerifyTokenResponse;
import com.azurion.saascore.crm.application.dto.WhatsappUnreadSummaryResponse;
import com.azurion.saascore.crm.application.services.WhatsappConfigurationService;
import com.azurion.saascore.crm.application.services.WhatsappIntegrationService;
import com.azurion.saascore.modulos.application.services.RequireModule;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/v1/saas/crm", "/crm"})
@RequiredArgsConstructor
@RequireModule("CRM")
public class WhatsappCrmController {

    private final WhatsappIntegrationService whatsappIntegrationService;
    private final WhatsappConfigurationService whatsappConfigurationService;

    @GetMapping("/whatsapp/estado")
    @PreAuthorize("hasAnyAuthority('CRM_LEADS_READ','CRM_ACTIVITIES_READ','CRM_CONFIG_MANAGE')")
    public ApiResponse<WhatsappConnectionStatusResponse> getConnectionStatus() {
        return ApiResponse.ok(
                whatsappConfigurationService.getStatus(),
                "Estado de la conexion de WhatsApp"
        );
    }

    @PostMapping("/whatsapp/configuracion/verify-token")
    @PreAuthorize("hasAuthority('CRM_CONFIG_MANAGE')")
    public ApiResponse<WhatsappVerifyTokenResponse> generateVerifyToken() {
        return ApiResponse.ok(
                whatsappConfigurationService.generateVerifyToken(),
                "Token de verificacion de WhatsApp generado"
        );
    }

    @PostMapping("/whatsapp/configuracion/probar")
    @PreAuthorize("hasAuthority('CRM_CONFIG_MANAGE')")
    public ApiResponse<WhatsappConnectionStatusResponse> testConnection() {
        return ApiResponse.ok(
                whatsappConfigurationService.testConnection(),
                "Comprobacion de WhatsApp completada"
        );
    }

    @PostMapping("/whatsapp/configuracion/suscribir")
    @PreAuthorize("hasAuthority('CRM_CONFIG_MANAGE')")
    public ApiResponse<WhatsappConnectionStatusResponse> subscribeApp() {
        return ApiResponse.ok(
                whatsappConfigurationService.subscribeApp(),
                "Aplicacion suscrita a la cuenta de WhatsApp Business"
        );
    }

    @GetMapping("/whatsapp/conversaciones")
    @PreAuthorize("hasAnyAuthority('CRM_LEADS_READ','CRM_ACTIVITIES_READ')")
    public ApiResponse<List<CrmWhatsappConversationResponse>> listConversations(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "false") boolean soloNoLeidas,
            @RequestParam(defaultValue = "false") boolean soloMias) {
        return ApiResponse.ok(
                whatsappIntegrationService.listConversations(query, estado, soloNoLeidas, soloMias),
                "Bandeja de conversaciones de WhatsApp"
        );
    }

    @GetMapping("/whatsapp/notificaciones")
    @PreAuthorize("hasAnyAuthority('CRM_LEADS_READ','CRM_ACTIVITIES_READ')")
    public ApiResponse<WhatsappUnreadSummaryResponse> unreadNotifications() {
        return ApiResponse.ok(
                whatsappIntegrationService.getUnreadSummary(),
                "Mensajes de WhatsApp pendientes de lectura"
        );
    }

    @PutMapping("/whatsapp/conversaciones/{prospectoId}/leer")
    @PreAuthorize("hasAnyAuthority('CRM_LEADS_READ','CRM_ACTIVITIES_READ')")
    public ApiResponse<CrmWhatsappConversationResponse> markAsRead(@PathVariable Long prospectoId) {
        return ApiResponse.ok(
                whatsappIntegrationService.markConversationRead(prospectoId),
                "Conversacion marcada como leida"
        );
    }

    @PutMapping("/whatsapp/conversaciones/{prospectoId}/estado")
    @PreAuthorize("hasAnyAuthority('CRM_LEADS_WRITE','CRM_ACTIVITIES_WRITE')")
    public ApiResponse<CrmWhatsappConversationResponse> updateStatus(
            @PathVariable Long prospectoId,
            @Valid @RequestBody UpdateWhatsappConversationStatusRequest request) {
        return ApiResponse.ok(
                whatsappIntegrationService.updateConversationStatus(prospectoId, request.estado()),
                "Estado de la conversacion actualizado"
        );
    }

    @PutMapping("/whatsapp/conversaciones/{prospectoId}/asignacion")
    @PreAuthorize("hasAnyAuthority('CRM_LEADS_WRITE','CRM_ACTIVITIES_WRITE')")
    public ApiResponse<CrmWhatsappConversationResponse> assign(
            @PathVariable Long prospectoId,
            @Valid @RequestBody AssignWhatsappConversationRequest request) {
        return ApiResponse.ok(
                whatsappIntegrationService.assignConversation(prospectoId, request.responsableId()),
                "Responsable de la conversacion actualizado"
        );
    }

    @PutMapping("/whatsapp/conversaciones/{prospectoId}/nota")
    @PreAuthorize("hasAnyAuthority('CRM_LEADS_WRITE','CRM_ACTIVITIES_WRITE')")
    public ApiResponse<CrmWhatsappConversationResponse> updateNote(
            @PathVariable Long prospectoId,
            @Valid @RequestBody UpdateWhatsappConversationNoteRequest request) {
        return ApiResponse.ok(
                whatsappIntegrationService.updateConversationNote(prospectoId, request.nota()),
                "Nota interna de la conversacion actualizada"
        );
    }

    @GetMapping("/prospectos/{prospectoId}/whatsapp/mensajes")
    @PreAuthorize("hasAnyAuthority('CRM_LEADS_READ','CRM_ACTIVITIES_READ')")
    public ApiResponse<List<CrmWhatsappMessageResponse>> listMessages(@PathVariable Long prospectoId) {
        return ApiResponse.ok(
                whatsappIntegrationService.listMessages(prospectoId),
                "Conversacion de WhatsApp del prospecto"
        );
    }

    @PostMapping("/prospectos/{prospectoId}/whatsapp/mensajes")
    @PreAuthorize("hasAnyAuthority('CRM_LEADS_WRITE','CRM_ACTIVITIES_WRITE')")
    public ApiResponse<CrmWhatsappMessageResponse> sendMessage(
            @PathVariable Long prospectoId,
            @Valid @RequestBody SendWhatsappMessageRequest request) {
        return ApiResponse.ok(
                whatsappIntegrationService.sendMessage(prospectoId, request),
                "Mensaje enviado a WhatsApp"
        );
    }
}
