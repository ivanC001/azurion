package com.azurion.saascore.modulos.application.usecases;

import com.azurion.saascore.modulos.application.dto.ModuloResponse;
import com.azurion.saascore.modulos.application.mappers.ModuloMapper;
import com.azurion.saascore.modulos.domain.repositories.ModuloRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListModulosUseCase {

    private final ModuloRepository moduloRepository;

    public List<ModuloResponse> execute() {
        return moduloRepository.findAllByOrderByNombreAsc().stream()
                .map(ModuloMapper::toResponse)
                .toList();
    }
}
