package com.azurion.saascore.roles.application.usecases;

import com.azurion.saascore.roles.application.dto.PermisoResponse;
import com.azurion.saascore.roles.application.dto.UpdatePermisoRequest;
import com.azurion.saascore.roles.application.mappers.RolesMapper;
import com.azurion.saascore.roles.application.support.RolesBusinessRules;
import com.azurion.saascore.roles.domain.entities.Permiso;
import com.azurion.saascore.roles.domain.repositories.PermisoRepository;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdatePermisoUseCase {

    private final PermisoRepository permisoRepository;

    public PermisoResponse execute(Long id, UpdatePermisoRequest request) {
        Permiso permiso = permisoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("PERMISO_NO_ENCONTRADO", "Permiso no encontrado"));

        if (!RolesBusinessRules.canEditPermiso(permiso)) {
            throw new BusinessException("PERMISO_RESERVADO", "No se puede editar un permiso base del sistema");
        }

        permiso.setNombre(request.nombre().trim());
        permiso.setDescripcion(request.descripcion());
        permiso.setModulo(RolesBusinessRules.normalizeIdentifier(request.modulo()));
        if (request.activo() != null) {
            permiso.setActivo(request.activo());
        }

        return RolesMapper.toPermisoResponse(permisoRepository.save(permiso));
    }
}
