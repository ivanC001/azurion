package com.azurion.saascore.crm.presentation.controllers;

import com.azurion.saascore.crm.application.dto.PublicCrmLeadRequest;
import com.azurion.saascore.crm.application.dto.PublicCrmCatalogoItemResponse;
import com.azurion.saascore.crm.application.dto.PublicLeadReceiptResponse;
import com.azurion.saascore.crm.application.services.CrmLandingIngressRegistryService;
import com.azurion.saascore.crm.application.services.PublicCrmTenantResolver;
import com.azurion.saascore.crm.application.usecases.CrmUseCaseService;
import com.azurion.shared.api.ApiResponse;
import com.azurion.shared.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/v1/public/crm", "/public/crm"})
@RequiredArgsConstructor
public class PublicCrmLeadController {

    private final CrmUseCaseService crmUseCaseService;
    private final PublicCrmTenantResolver tenantResolver;
    private final CrmLandingIngressRegistryService ingressRegistryService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @PostMapping("/leads")
    public ApiResponse<PublicLeadReceiptResponse> capture(
            @RequestHeader(name = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PublicCrmLeadRequest request) {
        tenantResolver.resolveWithContextFallback(request.rucTenant());
        return ApiResponse.ok(
                crmUseCaseService.capturePublicLead(request, "LEGACY", idempotencyKey),
                "Lead CRM recibido"
        );
    }

    @PostMapping("/leads/relay")
    public ApiResponse<PublicLeadReceiptResponse> relay(
            @RequestHeader("X-Azurion-Source-Key") String sourceKey,
            @RequestHeader("X-Azurion-Timestamp") String timestamp,
            @RequestHeader("X-Azurion-Signature") String signature,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @RequestBody String rawBody) {
        CrmLandingIngressRegistryService.ResolvedIngress ingress = ingressRegistryService.verifyRelay(
                sourceKey,
                timestamp,
                idempotencyKey,
                rawBody,
                signature
        );
        tenantResolver.resolve(ingress.tenantId());
        PublicCrmLeadRequest request = deserializeAndValidate(rawBody)
                .forIngress(ingress.tenantId(), ingress.sourceKey());
        return ApiResponse.ok(
                crmUseCaseService.capturePublicLead(request, "SERVER", idempotencyKey),
                "Lead CRM recibido por relay seguro"
        );
    }

    @GetMapping("/catalogo/{id}")
    public ApiResponse<PublicCrmCatalogoItemResponse> catalogo(@PathVariable Long id,
                                                               @RequestParam(required = false) String tenant,
                                                               @RequestParam(name = "Ruc_tenant", required = false) String rucTenant,
                                                               @RequestParam String token) {
        tenantResolver.resolveWithContextFallback(firstNonBlank(rucTenant, tenant));
        return ApiResponse.ok(crmUseCaseService.getPublicCatalogoItem(id, token), "Oferta CRM publica");
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return second == null ? null : second.trim();
    }

    private PublicCrmLeadRequest deserializeAndValidate(String rawBody) {
        try {
            PublicCrmLeadRequest request = objectMapper.readValue(rawBody, PublicCrmLeadRequest.class);
            var violations = validator.validate(request);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
            return request;
        } catch (JsonProcessingException ex) {
            throw new BusinessException("CRM_RELAY_JSON_INVALIDO", "El cuerpo JSON del relay no es valido");
        }
    }
}
