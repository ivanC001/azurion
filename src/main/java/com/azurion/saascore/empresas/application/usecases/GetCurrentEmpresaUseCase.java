package com.azurion.saascore.empresas.application.usecases;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.empresas.application.dto.EmpresaResponse;
import com.azurion.saascore.empresas.application.mappers.EmpresaMapper;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetCurrentEmpresaUseCase {

    private final EmpresaRepository empresaRepository;

    public EmpresaResponse execute() {
        return toResponse(resolveCurrentEmpresa());
    }

    public Empresa resolveCurrentEmpresa() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank() || TenantContext.DEFAULT_TENANT.equalsIgnoreCase(tenantId)) {
            throw new BusinessException("EMPRESA_CONTEXT_INVALID", "No existe una empresa tenant activa en la sesion.");
        }

        return empresaRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new BusinessException("EMPRESA_NO_ENCONTRADA", "Empresa no encontrada para el tenant actual."));
    }

    public EmpresaResponse toResponse(Empresa empresa) {
        return EmpresaMapper.toResponse(empresa);
    }
}
