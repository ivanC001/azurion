package com.azurion.saascore.usuarios.application.services;

import com.azurion.multitenancy.TenantContext;
import com.azurion.shared.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class TenantRoleAssignmentAuthorizer {

    public void assertCanAssign(String tenantId, String normalizedRoleCode) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }

        boolean isGeneralAdmin = RoleCodeSupport.hasAnyRole(authentication, "ADMIN_GENERAL", "PLATFORM_ADMIN");
        boolean isCompanyAdmin = RoleCodeSupport.hasAnyRole(authentication, "ADMIN_EMPRESA", "ADMIN");

        if (!isGeneralAdmin && !isCompanyAdmin) {
            throw new BusinessException("ACCESO_DENEGADO", "No tiene permisos para asignar roles de tenant");
        }

        if (!isGeneralAdmin) {
            String currentTenant = TenantContext.getTenantId();
            if (TenantContext.DEFAULT_TENANT.equalsIgnoreCase(currentTenant)
                    || !currentTenant.equalsIgnoreCase(tenantId)) {
                throw new BusinessException("ACCESO_DENEGADO", "Solo puede asignar roles dentro de su propio tenant");
            }
        }

        if ("ADMIN_EMPRESA".equalsIgnoreCase(normalizedRoleCode) && !isGeneralAdmin) {
            throw new BusinessException("ACCESO_DENEGADO", "Solo el administrador general puede asignar ADMIN_EMPRESA");
        }
    }

    public void assertCanRead(String tenantId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }

        boolean isGeneralAdmin = RoleCodeSupport.hasAnyRole(authentication, "ADMIN_GENERAL", "PLATFORM_ADMIN");
        if (isGeneralAdmin) {
            return;
        }

        boolean isCompanyAdmin = RoleCodeSupport.hasAnyRole(authentication, "ADMIN_EMPRESA", "ADMIN");
        if (!isCompanyAdmin) {
            throw new BusinessException("ACCESO_DENEGADO", "No tiene permisos para consultar roles de tenant");
        }

        String currentTenant = TenantContext.getTenantId();
        if (TenantContext.DEFAULT_TENANT.equalsIgnoreCase(currentTenant)
                || !currentTenant.equalsIgnoreCase(tenantId)) {
            throw new BusinessException("ACCESO_DENEGADO", "Solo puede consultar su propio tenant");
        }
    }
}
