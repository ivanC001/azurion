package com.azurion.saascore.roles.application.usecases;

import com.azurion.saascore.roles.application.dto.PermisoResponse;
import com.azurion.saascore.roles.application.mappers.RolesMapper;
import com.azurion.saascore.roles.domain.repositories.PermisoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListPermisosUseCase {

    private final PermisoRepository permisoRepository;

    public List<PermisoResponse> execute() {
        return permisoRepository.findAllByOrderByModuloAscNombreAsc().stream()
                .map(RolesMapper::toPermisoResponse)
                .toList();
    }
}
