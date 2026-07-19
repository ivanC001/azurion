package com.azurion.saascore.roles.application.usecases;

import com.azurion.saascore.roles.application.dto.CreateRolRequest;
import com.azurion.saascore.roles.application.dto.RolResponse;
import com.azurion.saascore.roles.application.mappers.RolesMapper;
import com.azurion.saascore.roles.application.support.RolesBusinessRules;
import com.azurion.saascore.roles.domain.entities.Rol;
import com.azurion.saascore.roles.domain.entities.RoleScope;
import com.azurion.saascore.roles.domain.repositories.RolRepository;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateRolUseCase {

    private final RolRepository rolRepository;

    public RolResponse execute(CreateRolRequest request) {
        String normalizedCode = RolesBusinessRules.normalizeRoleCode(request.codigo());
        if (normalizedCode.isBlank()) {
            throw new BusinessException("ROL_INVALIDO", "El codigo del rol no es valido");
        }

        RolesBusinessRules.ensureCustomRoleCode(normalizedCode);

        if (rolRepository.existsByCodigoIgnoreCase(normalizedCode)) {
            throw new BusinessException("ROL_DUPLICADO", "Ya existe un rol con ese codigo");
        }

        Rol rol = new Rol();
        rol.setCodigo(normalizedCode);
        rol.setNombre(request.nombre().trim());
        rol.setDescripcion(request.descripcion());
        rol.setAmbito(request.ambito() == null ? RoleScope.ERP : request.ambito());
        rol.setActivo(true);
        rol.setSistema(false);
        rol.setDeprecated(false);

        return RolesMapper.toRolResponse(rolRepository.save(rol));
    }
}
