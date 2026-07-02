package com.azurion.saascore.roles.application.usecases;

import com.azurion.saascore.roles.application.dto.RolResponse;
import com.azurion.saascore.roles.application.mappers.RolesMapper;
import com.azurion.saascore.roles.application.support.RolesBusinessRules;
import com.azurion.saascore.roles.domain.entities.Permiso;
import com.azurion.saascore.roles.domain.entities.Rol;
import com.azurion.saascore.roles.domain.entities.RolPermiso;
import com.azurion.saascore.roles.domain.repositories.PermisoRepository;
import com.azurion.saascore.roles.domain.repositories.RolRepository;
import com.azurion.shared.exception.BusinessException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SyncRolPermisosUseCase {

    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;

    @Transactional
    public RolResponse execute(Long rolId, List<Long> permisoIds) {
        Rol rol = rolRepository.findWithPermisosById(rolId)
                .orElseThrow(() -> new BusinessException("ROL_NO_ENCONTRADO", "Rol no encontrado"));

        if (!RolesBusinessRules.canManagePermissions(rol)) {
            throw new BusinessException("ROL_RESERVADO", "Los roles administrativos conservan permisos gestionados por el sistema");
        }

        List<Permiso> permisos = permisoRepository.findByIdIn(permisoIds);
        if (permisos.size() != permisoIds.size()) {
            throw new BusinessException("PERMISOS_INVALIDOS", "Uno o mas permisos no existen");
        }
        if (permisos.stream().anyMatch(permiso -> !permiso.isActivo())) {
            throw new BusinessException("PERMISOS_INACTIVOS", "No se pueden asignar permisos inactivos");
        }

        Set<Long> requestedIds = new HashSet<>(permisoIds);
        rol.getRolPermisos().removeIf(rp -> !requestedIds.contains(rp.getPermiso().getId()));

        Set<Long> currentIds = rol.getRolPermisos().stream()
                .map(rp -> rp.getPermiso().getId())
                .collect(java.util.stream.Collectors.toSet());

        for (Permiso permiso : permisos) {
            if (!currentIds.contains(permiso.getId())) {
                RolPermiso rolPermiso = new RolPermiso();
                rolPermiso.setRol(rol);
                rolPermiso.setPermiso(permiso);
                rol.getRolPermisos().add(rolPermiso);
            }
        }

        Rol saved = rolRepository.save(rol);
        return RolesMapper.toRolResponse(saved);
    }
}
