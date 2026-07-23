package com.azurion.saascore.crm.presentation.controllers;

import com.azurion.saascore.crm.application.dto.CrmProspectoResponse;
import com.azurion.saascore.crm.application.dto.PublicCrmLeadRequest;
import com.azurion.saascore.crm.application.dto.PublicCrmCatalogoItemResponse;
import com.azurion.saascore.crm.application.services.PublicCrmTenantResolver;
import com.azurion.saascore.crm.application.usecases.CrmUseCaseService;
import com.azurion.shared.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/v1/public/crm", "/public/crm"})
@RequiredArgsConstructor
public class PublicCrmLeadController {

    private final CrmUseCaseService crmUseCaseService;
    private final PublicCrmTenantResolver tenantResolver;

    @PostMapping("/leads")
    public ApiResponse<CrmProspectoResponse> capture(@Valid @RequestBody PublicCrmLeadRequest request) {
        tenantResolver.resolveWithContextFallback(request.rucTenant());
        return ApiResponse.ok(crmUseCaseService.capturePublicLead(request), "Lead CRM registrado");
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
}
