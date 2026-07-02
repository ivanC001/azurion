package com.azurion.saascore.roles.application.usecases;

import com.azurion.saascore.roles.application.support.RolesBusinessRules;
import com.azurion.saascore.roles.domain.entities.Permiso;
import com.azurion.saascore.roles.domain.repositories.PermisoRepository;
import com.azurion.saascore.roles.domain.repositories.RolPermisoRepository;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeletePermisoUseCase {

    private final PermisoRepository permisoRepository;
    private final RolPermisoRepository rolPermisoRepository;

    @Transactional
    public void execute(Long id) {
        Permiso permiso = permisoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("PERMISO_NO_ENCONTRADO", "Permiso no encontrado"));

        if (!RolesBusinessRules.canDeletePermiso(permiso)) {
            throw new BusinessException("PERMISO_RESERVADO", "No se puede eliminar un permiso base del sistema");
        }

        if (rolPermisoRepository.countByPermiso_Id(id) > 0) {
            throw new BusinessException("PERMISO_EN_USO", "No se puede eliminar: permiso asignado a roles");
        }
        permisoRepository.deleteById(id);
    }
}
