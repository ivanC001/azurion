package com.azurion.saascore.roles.application.usecases;

import com.azurion.saascore.roles.application.dto.RolResponse;
import com.azurion.saascore.roles.application.mappers.RolesMapper;
import com.azurion.saascore.roles.application.support.RolesBusinessRules;
import com.azurion.saascore.roles.domain.entities.Permiso;
import com.azurion.saascore.roles.domain.entities.Rol;
import com.azurion.saascore.roles.domain.entities.RolPermiso;
import com.azurion.saascore.roles.domain.repositories.PermisoRepository;
import com.azurion.saascore.roles.domain.repositories.RolPermisoRepository;
import com.azurion.saascore.roles.domain.repositories.RolRepository;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddPermisoToRolUseCase {

    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final RolPermisoRepository rolPermisoRepository;

    @Transactional
    public RolResponse execute(Long rolId, Long permisoId) {
        Rol rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new BusinessException("ROL_NO_ENCONTRADO", "Rol no encontrado"));

        if (!RolesBusinessRules.canManagePermissions(rol)) {
            throw new BusinessException("ROL_RESERVADO", "Los roles administrativos conservan permisos gestionados por el sistema");
        }

        Permiso permiso = permisoRepository.findById(permisoId)
                .orElseThrow(() -> new BusinessException("PERMISO_NO_ENCONTRADO", "Permiso no encontrado"));

        if (!permiso.isActivo()) {
            throw new BusinessException("PERMISO_INACTIVO", "No se puede asignar un permiso inactivo");
        }

        if (!rolPermisoRepository.existsByRol_IdAndPermiso_Id(rolId, permisoId)) {
            RolPermiso rolPermiso = new RolPermiso();
            rolPermiso.setRol(rol);
            rolPermiso.setPermiso(permiso);
            rolPermisoRepository.save(rolPermiso);
        }

        Rol refreshed = rolRepository.findWithPermisosById(rolId)
                .orElseThrow(() -> new BusinessException("ROL_NO_ENCONTRADO", "Rol no encontrado"));
        return RolesMapper.toRolResponse(refreshed);
    }
}
