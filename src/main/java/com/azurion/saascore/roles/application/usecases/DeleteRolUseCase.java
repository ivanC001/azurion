package com.azurion.saascore.roles.application.usecases;

import com.azurion.saascore.roles.application.support.RolesBusinessRules;
import com.azurion.saascore.roles.domain.entities.Rol;
import com.azurion.saascore.roles.domain.repositories.RolRepository;
import com.azurion.saascore.usuarios.domain.repositories.UsuarioRolRepository;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeleteRolUseCase {

    private final RolRepository rolRepository;
    private final UsuarioRolRepository usuarioRolRepository;

    public void execute(Long id) {
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new BusinessException("ROL_NO_ENCONTRADO", "Rol no encontrado"));

        if (!RolesBusinessRules.canDeleteRole(rol)) {
            throw new BusinessException("ROL_RESERVADO", "No se puede eliminar un rol base del sistema");
        }

        if (usuarioRolRepository.countByRolId(id) > 0) {
            throw new BusinessException("ROL_EN_USO", "No se puede eliminar: rol asignado a usuarios");
        }

        rolRepository.delete(rol);
    }
}
