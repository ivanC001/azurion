package com.azurion.saascore.crm.application.services;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.saascore.modulos.application.services.ModuleAccessService;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PublicCrmTenantResolver {

    private final EmpresaRepository empresaRepository;
    private final ModuleAccessService moduleAccessService;

    public Empresa resolve(String tenantReference) {
        return resolve(tenantReference, false);
    }

    public Empresa resolveWithContextFallback(String tenantReference) {
        return resolve(tenantReference, true);
    }

    private Empresa resolve(String explicitTenantReference, boolean allowContextFallback) {
        String currentTenant = TenantContext.getTenantId();
        String tenantReference = normalize(explicitTenantReference);
        if (tenantReference == null && allowContextFallback && !TenantContext.DEFAULT_TENANT.equalsIgnoreCase(currentTenant)) {
            tenantReference = normalize(currentTenant);
        }
        if (tenantReference == null) {
            throw new BusinessException("CRM_TENANT_REQUERIDO", "Envia Ruc_tenant, tenant o el header X-Tenant-Id");
        }

        String reference = tenantReference;
        Empresa empresa = empresaRepository.findByRucIgnoreCase(reference)
                .or(() -> empresaRepository.findByTenantId(reference))
                .orElseThrow(() -> new BusinessException(
                        "CRM_TENANT_NO_ENCONTRADO",
                        "No existe empresa para el tenant indicado"
                ));
        if (!empresa.isActivo()) {
            throw new BusinessException("CRM_TENANT_INACTIVO", "La empresa no esta activa para operar el CRM");
        }

        if (!empresa.getTenantId().equals(currentTenant)) {
            TenantContext.setTenantId(empresa.getTenantId());
        }
        moduleAccessService.requireModule(empresa.getId(), "CRM");
        return empresa;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
