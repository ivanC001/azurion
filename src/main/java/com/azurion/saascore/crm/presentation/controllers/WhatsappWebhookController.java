package com.azurion.saascore.crm.presentation.controllers;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.crm.application.dto.WhatsappWebhookResult;
import com.azurion.saascore.crm.application.services.WhatsappIntegrationService;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.saascore.modulos.application.services.ModuleAccessService;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/v1/public/crm/whatsapp", "/public/crm/whatsapp"})
@RequiredArgsConstructor
public class WhatsappWebhookController {

    private final WhatsappIntegrationService whatsappIntegrationService;
    private final EmpresaRepository empresaRepository;
    private final ModuleAccessService moduleAccessService;

    @GetMapping(value = "/{tenantReference}/webhook", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verify(
            @PathVariable String tenantReference,
            @RequestParam(name = "hub.mode") String mode,
            @RequestParam(name = "hub.verify_token") String verifyToken,
            @RequestParam(name = "hub.challenge") String challenge) {
        resolveTenant(tenantReference);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(whatsappIntegrationService.verifyWebhook(mode, verifyToken, challenge));
    }

    @PostMapping(value = "/{tenantReference}/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> receive(
            @PathVariable String tenantReference,
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String rawBody) {
        resolveTenant(tenantReference);
        WhatsappWebhookResult result = whatsappIntegrationService.processWebhook(rawBody, signature);
        return ResponseEntity.ok("EVENT_RECEIVED:" + result.mensajesProcesados());
    }

    private void resolveTenant(String tenantReference) {
        if (tenantReference == null || tenantReference.isBlank()) {
            throw new BusinessException("CRM_TENANT_REQUERIDO", "El webhook requiere el tenant en la URL");
        }
        Empresa empresa = empresaRepository.findByRuc(tenantReference.trim())
                .or(() -> empresaRepository.findByTenantId(tenantReference.trim()))
                .orElseThrow(() -> new BusinessException(
                        "CRM_TENANT_NO_ENCONTRADO",
                        "No existe empresa para el tenant indicado"
                ));
        if (!empresa.isActivo()) {
            throw new BusinessException("CRM_TENANT_INACTIVO", "La empresa no esta activa para captar leads");
        }
        TenantContext.setTenantId(empresa.getTenantId());
        moduleAccessService.requireModule(empresa.getId(), "CRM");
    }
}
