package com.azurion.saascore.empresas.application.usecases;

import com.azurion.saascore.empresas.application.dto.EmpresaResponse;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetEmpresaByIdUseCase {

    private final EmpresaRepository empresaRepository;

    public EmpresaResponse execute(Long id) {
        var empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new BusinessException("EMPRESA_NO_ENCONTRADA", "Empresa no encontrada"));

        return new EmpresaResponse(
                empresa.getId(),
                empresa.getRuc(),
                empresa.getRazonSocial(),
                empresa.getTenantId(),
                empresa.getSchemaName(),
                empresa.getLogoPanelUrl(),
                empresa.isActivo()
        );
    }
}
