package com.azurion.saascore.roles.application.usecases;

import com.azurion.saascore.roles.application.dto.PermisoResponse;
import com.azurion.saascore.roles.application.mappers.RolesMapper;
import com.azurion.saascore.roles.domain.entities.Permiso;
import com.azurion.saascore.roles.domain.repositories.PermisoRepository;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetPermisoByIdUseCase {

    private final PermisoRepository permisoRepository;

    public PermisoResponse execute(Long id) {
        Permiso permiso = permisoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("PERMISO_NO_ENCONTRADO", "Permiso no encontrado"));
        return RolesMapper.toPermisoResponse(permiso);
    }
}
