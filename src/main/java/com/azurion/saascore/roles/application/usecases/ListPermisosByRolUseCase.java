package com.azurion.saascore.roles.application.usecases;

import com.azurion.saascore.roles.application.dto.PermisoResponse;
import com.azurion.saascore.roles.application.mappers.RolesMapper;
import com.azurion.saascore.roles.domain.repositories.RolPermisoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListPermisosByRolUseCase {

    private final RolPermisoRepository rolPermisoRepository;

    public List<PermisoResponse> execute(Long rolId) {
        return rolPermisoRepository.findByRol_IdOrderByPermiso_ModuloAscPermiso_NombreAsc(rolId).stream()
                .map(rolPermiso -> RolesMapper.toPermisoResponse(rolPermiso.getPermiso()))
                .toList();
    }
}
