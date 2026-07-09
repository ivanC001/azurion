package com.azurion.saascore.crm.presentation.controllers;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.crm.application.dto.CrmProspectoResponse;
import com.azurion.saascore.crm.application.dto.PublicCrmLeadRequest;
import com.azurion.saascore.crm.application.dto.PublicCrmCatalogoItemResponse;
import com.azurion.saascore.crm.application.usecases.CrmUseCaseService;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.shared.api.ApiResponse;
import com.azurion.shared.exception.BusinessException;
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
    private final EmpresaRepository empresaRepository;

    @PostMapping("/leads")
    public ApiResponse<CrmProspectoResponse> capture(@Valid @RequestBody PublicCrmLeadRequest request) {
        resolveTenantFromRequest(request);
        return ApiResponse.ok(crmUseCaseService.capturePublicLead(request), "Lead CRM registrado");
    }

    @GetMapping("/catalogo/{id}")
    public ApiResponse<PublicCrmCatalogoItemResponse> catalogo(@PathVariable Long id,
                                                               @RequestParam(required = false) String tenant,
                                                               @RequestParam(name = "Ruc_tenant", required = false) String rucTenant,
                                                               @RequestParam String token) {
        resolveTenantReference(firstNonBlank(rucTenant, tenant));
        return ApiResponse.ok(crmUseCaseService.getPublicCatalogoItem(id, token), "Oferta CRM publica");
    }

    private void resolveTenantFromRequest(PublicCrmLeadRequest request) {
        resolveTenantReference(request.rucTenant());
    }

    private void resolveTenantReference(String explicitTenantReference) {
        String currentTenant = TenantContext.getTenantId();
        String tenantReference = firstNonBlank(explicitTenantReference, TenantContext.DEFAULT_TENANT.equalsIgnoreCase(currentTenant) ? null : currentTenant);
        if (tenantReference == null || tenantReference.isBlank()) {
            throw new BusinessException("CRM_TENANT_REQUERIDO", "Envia Ruc_tenant, tenant o el header X-Tenant-Id");
        }
        Empresa empresa = empresaRepository.findByRuc(tenantReference)
                .or(() -> empresaRepository.findByTenantId(tenantReference))
                .orElseThrow(() -> new BusinessException("CRM_TENANT_NO_ENCONTRADO", "No existe empresa para tenant: " + tenantReference));
        if (!empresa.isActivo()) {
            throw new BusinessException("CRM_TENANT_INACTIVO", "La empresa no esta activa para captar leads");
        }
        if (empresa.getTenantId().equals(currentTenant)) {
            return;
        }
        TenantContext.setTenantId(empresa.getTenantId());
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return second == null ? null : second.trim();
    }
}
