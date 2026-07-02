package com.azurion.saascore.roles.application.usecases;

import com.azurion.saascore.roles.application.dto.RolResponse;
import com.azurion.saascore.roles.application.dto.UpdateRolRequest;
import com.azurion.saascore.roles.application.mappers.RolesMapper;
import com.azurion.saascore.roles.application.support.RolesBusinessRules;
import com.azurion.saascore.roles.domain.entities.Rol;
import com.azurion.saascore.roles.domain.repositories.RolRepository;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateRolUseCase {

    private final RolRepository rolRepository;

    public RolResponse execute(Long id, UpdateRolRequest request) {
        Rol rol = rolRepository.findWithPermisosById(id)
                .orElseThrow(() -> new BusinessException("ROL_NO_ENCONTRADO", "Rol no encontrado"));

        if (!RolesBusinessRules.canEditRole(rol)) {
            throw new BusinessException("ROL_RESERVADO", "No se puede editar un rol administrativo del sistema");
        }

        rol.setNombre(request.nombre().trim());
        rol.setDescripcion(request.descripcion());
        if (request.activo() != null) {
            rol.setActivo(request.activo());
        }

        return RolesMapper.toRolResponse(rolRepository.save(rol));
    }
}
