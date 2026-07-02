package com.azurion.saascore.roles.application.usecases;

import com.azurion.saascore.roles.application.dto.RolResponse;
import com.azurion.saascore.roles.application.mappers.RolesMapper;
import com.azurion.saascore.roles.application.support.RolesBusinessRules;
import com.azurion.saascore.roles.domain.entities.Rol;
import com.azurion.saascore.roles.domain.repositories.RolPermisoRepository;
import com.azurion.saascore.roles.domain.repositories.RolRepository;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RemovePermisoFromRolUseCase {

    private final RolRepository rolRepository;
    private final RolPermisoRepository rolPermisoRepository;

    @Transactional
    public RolResponse execute(Long rolId, Long permisoId) {
        Rol rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new BusinessException("ROL_NO_ENCONTRADO", "Rol no encontrado"));

        if (!RolesBusinessRules.canManagePermissions(rol)) {
            throw new BusinessException("ROL_RESERVADO", "Los roles administrativos conservan permisos gestionados por el sistema");
        }

        rolPermisoRepository.deleteByRol_IdAndPermiso_Id(rolId, permisoId);

        Rol refreshed = rolRepository.findWithPermisosById(rolId)
                .orElseThrow(() -> new BusinessException("ROL_NO_ENCONTRADO", "Rol no encontrado"));
        return RolesMapper.toRolResponse(refreshed);
    }
}
