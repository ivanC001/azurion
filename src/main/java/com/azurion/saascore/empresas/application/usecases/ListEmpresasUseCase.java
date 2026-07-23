package com.azurion.saascore.empresas.application.usecases;

import com.azurion.saascore.empresas.application.dto.EmpresaResponse;
import com.azurion.saascore.empresas.application.mappers.EmpresaMapper;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListEmpresasUseCase {

    private final EmpresaRepository empresaRepository;

    public List<EmpresaResponse> execute() {
        return empresaRepository.findAll().stream()
                .map(EmpresaMapper::toResponse)
                .toList();
    }
}
