package com.azurion.saascore.modulos.application.usecases;

import com.azurion.saascore.modulos.application.dto.ModuloResponse;
import com.azurion.saascore.modulos.application.mappers.ModuloMapper;
import com.azurion.saascore.modulos.domain.repositories.ModuloRepository;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetModuloByIdUseCase {

    private final ModuloRepository moduloRepository;

    public ModuloResponse execute(Long id) {
        return moduloRepository.findById(id)
                .map(ModuloMapper::toResponse)
                .orElseThrow(() -> new BusinessException("MODULO_NOT_FOUND", "Modulo not found: " + id));
    }
}
