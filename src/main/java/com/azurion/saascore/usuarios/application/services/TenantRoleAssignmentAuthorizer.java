package com.azurion.saascore.usuarios.application.services;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.modulos.application.services.ModuleAccessService;
import com.azurion.saascore.roles.domain.entities.Rol;
import com.azurion.saascore.roles.domain.entities.RoleScope;
import com.azurion.saascore.roles.domain.repositories.RolRepository;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TenantRoleAssignmentAuthorizer {

    private final RolRepository rolRepository;
    private final ModuleAccessService moduleAccessService;

    public void assertCanAssign(String tenantId, String normalizedRoleCode) {
        AssignmentContext context = assertCanManageTenant(tenantId);
        if (context == null) {
            return;
        }

        if ("ADMIN_EMPRESA".equalsIgnoreCase(normalizedRoleCode) && !context.generalAdmin()) {
            throw new BusinessException("ACCESO_DENEGADO", "Solo el administrador general puede asignar ADMIN_EMPRESA");
        }

        Rol role = rolRepository.findByCodigoIgnoreCase(normalizedRoleCode)
                .orElseThrow(() -> new BusinessException("ROL_NO_ENCONTRADO", "Rol no encontrado"));
        if (role.isDeprecated()) {
            throw new BusinessException(
                    "ROL_DEPRECADO",
                    "El rol " + normalizedRoleCode + " es legado; selecciona un rol ERP_* o CRM_*"
            );
        }
        ensureActiveProduct(role.getAmbito());
    }

    public void assertCanRemove(String tenantId) {
        assertCanManageTenant(tenantId);
    }

    private AssignmentContext assertCanManageTenant(String tenantId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        boolean isGeneralAdmin = RoleCodeSupport.hasAnyRole(authentication, "ADMIN_GENERAL", "PLATFORM_ADMIN");
        boolean isCompanyAdmin = RoleCodeSupport.hasAnyRole(authentication, "ADMIN_EMPRESA");

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

        return new AssignmentContext(isGeneralAdmin);
    }

    private void ensureActiveProduct(RoleScope scope) {
        if (scope == RoleScope.ERP && !moduleAccessService.hasCurrentTenantModule("ERP")) {
            throw new BusinessException("MODULO_NO_ACTIVO", "No se puede asignar un rol ERP sin el modulo ERP activo");
        }
        if (scope == RoleScope.CRM && !moduleAccessService.hasCurrentTenantModule("CRM")) {
            throw new BusinessException("MODULO_NO_ACTIVO", "No se puede asignar un rol CRM sin el modulo CRM activo");
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

        boolean isCompanyAdmin = RoleCodeSupport.hasAnyRole(authentication, "ADMIN_EMPRESA");
        if (!isCompanyAdmin) {
            throw new BusinessException("ACCESO_DENEGADO", "No tiene permisos para consultar roles de tenant");
        }

        String currentTenant = TenantContext.getTenantId();
        if (TenantContext.DEFAULT_TENANT.equalsIgnoreCase(currentTenant)
                || !currentTenant.equalsIgnoreCase(tenantId)) {
            throw new BusinessException("ACCESO_DENEGADO", "Solo puede consultar su propio tenant");
        }
    }

    private record AssignmentContext(boolean generalAdmin) {
    }
}
