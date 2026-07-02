package com.azurion.saascore.configuracion.application.services;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmpresaModuloAdminAccessService {

    private static final Set<String> GENERAL_ADMIN_AUTHORITIES = Set.of("ROLE_PLATFORM_ADMIN", "ROLE_ADMIN_GENERAL");

    private final EmpresaRepository empresaRepository;

    public void requireViewAccess(Long empresaId) {
        if (isGeneralAdmin()) {
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities().stream().noneMatch(authority ->
                "ROLE_ADMIN_EMPRESA".equals(authority.getAuthority())
                        || "ROLE_ADMIN".equals(authority.getAuthority())
                        || "EMPRESA_MODULOS_READ".equals(authority.getAuthority()))) {
            throw new AccessDeniedException("No tiene permisos para ver los modulos contratados de la empresa");
        }

        Empresa empresaActual = empresaRepository.findByTenantId(TenantContext.getTenantId())
                .orElseThrow(() -> new AccessDeniedException("No se pudo resolver la empresa del tenant actual"));

        if (!empresaActual.getId().equals(empresaId)) {
            throw new AccessDeniedException("Solo puede consultar los modulos de su propia empresa");
        }
    }

    public void requireWriteAccess(Long empresaId) {
        if (isGeneralAdmin()) {
            return;
        }
        throw new AccessDeniedException("Solo ADMIN_GENERAL puede modificar modulos contratados");
    }

    private boolean isGeneralAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .anyMatch(GENERAL_ADMIN_AUTHORITIES::contains);
    }
}
