package com.azurion.saascore.modulos.application.services;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.configuracion.domain.repositories.EmpresaModuloRepository;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.shared.exception.BusinessException;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ModuleAccessService {

    private final EmpresaRepository empresaRepository;
    private final EmpresaModuloRepository empresaModuloRepository;

    public boolean hasModule(Long empresaId, String moduloCodigo) {
        if (empresaId == null || moduloCodigo == null || moduloCodigo.isBlank()) {
            return false;
        }
        return empresaModuloRepository.existsActiveModule(empresaId, normalizeCode(moduloCodigo), LocalDate.now());
    }

    public void requireModule(Long empresaId, String moduloCodigo) {
        if (!hasModule(empresaId, moduloCodigo)) {
            throw new AccessDeniedException("La empresa no tiene activo el modulo " + normalizeCode(moduloCodigo));
        }
    }

    public List<String> getActiveModules(Long empresaId) {
        if (empresaId == null) {
            return List.of();
        }
        return empresaModuloRepository.findActiveModuleCodes(empresaId, LocalDate.now());
    }

    public boolean hasCurrentTenantModule(String moduloCodigo) {
        return resolveCurrentEmpresaId()
                .map(empresaId -> hasModule(empresaId, moduloCodigo))
                .orElse(false);
    }

    public void requireCurrentTenantModule(String moduloCodigo) {
        Long empresaId = resolveCurrentEmpresaId()
                .orElseThrow(() -> new AccessDeniedException("No se pudo resolver la empresa actual para validar modulos"));
        requireModule(empresaId, moduloCodigo);
    }

    public List<String> getCurrentTenantActiveModules() {
        return resolveCurrentEmpresaId()
                .map(this::getActiveModules)
                .orElse(List.of());
    }

    public Long getCurrentTenantEmpresaId() {
        return resolveCurrentEmpresaId()
                .orElseThrow(() -> new BusinessException("EMPRESA_NOT_FOUND", "No se encontro la empresa asociada al tenant actual"));
    }

    public String getCurrentTenantId() {
        return TenantContext.getTenantId();
    }

    private java.util.Optional<Long> resolveCurrentEmpresaId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank() || TenantContext.DEFAULT_TENANT.equalsIgnoreCase(tenantId)) {
            return java.util.Optional.empty();
        }

        return empresaRepository.findByTenantId(tenantId)
                .map(Empresa::getId);
    }

    private String normalizeCode(String moduloCodigo) {
        return moduloCodigo.trim().toUpperCase();
    }
}
