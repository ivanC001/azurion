package com.azurion.saascore.crm.presentation.controllers;

import com.azurion.saascore.crm.application.dto.PublicCrmLeadRequest;
import com.azurion.saascore.crm.application.dto.PublicLeadReceiptResponse;
import com.azurion.saascore.crm.application.services.CrmLandingIngressRegistryService;
import com.azurion.saascore.crm.application.services.PublicCrmTenantResolver;
import com.azurion.saascore.crm.application.services.PublicLeadSubmissionAuditService;
import com.azurion.saascore.crm.application.usecases.CrmUseCaseService;
import com.azurion.shared.api.ApiResponse;
import com.azurion.shared.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/v1/public/forms", "/public/forms"})
@RequiredArgsConstructor
public class PublicCrmFormController {

    private final CrmLandingIngressRegistryService ingressRegistryService;
    private final PublicCrmTenantResolver tenantResolver;
    private final CrmUseCaseService crmUseCaseService;
    private final PublicLeadSubmissionAuditService submissionAuditService;

    @PostMapping("/{sourceKey}/submissions")
    public ResponseEntity<ApiResponse<PublicLeadReceiptResponse>> submit(
            @PathVariable String sourceKey,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PublicCrmLeadRequest request) {
        CrmLandingIngressRegistryService.ResolvedIngress ingress =
                ingressRegistryService.resolveBrowserSource(sourceKey);
        tenantResolver.resolve(ingress.tenantId());
        try {
            PublicLeadReceiptResponse receipt = crmUseCaseService.capturePublicLead(
                    request.forIngress(ingress.tenantId(), ingress.sourceKey()),
                    "BROWSER",
                    idempotencyKey
            );
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ApiResponse.ok(receipt, "Formulario recibido"));
        } catch (BusinessException ex) {
            submissionAuditService.recordRejected(
                    ingress.sourceKey(),
                    "BROWSER",
                    idempotencyKey,
                    ex.getCode(),
                    ex.isUserActionable() ? ex.getMessage() : null
            );
            throw ex;
        }
    }
}
