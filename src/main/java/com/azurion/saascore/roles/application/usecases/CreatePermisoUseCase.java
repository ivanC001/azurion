package com.azurion.saascore.roles.application.usecases;

import com.azurion.saascore.roles.application.dto.CreatePermisoRequest;
import com.azurion.saascore.roles.application.dto.PermisoResponse;
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
public class CreatePermisoUseCase {

    private final PermisoRepository permisoRepository;
    private final RolRepository rolRepository;
    private final RolPermisoRepository rolPermisoRepository;

    @Transactional
    public PermisoResponse execute(CreatePermisoRequest request) {
        String normalizedCode = RolesBusinessRules.normalizeIdentifier(request.codigo());
        String normalizedModule = RolesBusinessRules.normalizeIdentifier(request.modulo());
        if (normalizedCode.isBlank() || normalizedModule.isBlank()) {
            throw new BusinessException("PERMISO_INVALIDO", "El codigo y el modulo del permiso son obligatorios");
        }

        if (permisoRepository.existsByCodigoIgnoreCase(normalizedCode)) {
            throw new BusinessException("PERMISO_DUPLICADO", "Ya existe un permiso con ese codigo");
        }

        Permiso permiso = new Permiso();
        permiso.setCodigo(normalizedCode);
        permiso.setNombre(request.nombre().trim());
        permiso.setDescripcion(request.descripcion());
        permiso.setModulo(normalizedModule);
        permiso.setActivo(true);
        permiso.setSistema(false);

        Permiso saved = permisoRepository.save(permiso);
        grantAdministrativeRoles(saved);
        return RolesMapper.toPermisoResponse(saved);
    }

    private void grantAdministrativeRoles(Permiso permiso) {
        for (String roleCode : RolesBusinessRules.ADMIN_ROLE_CODES) {
            rolRepository.findByCodigoIgnoreCase(roleCode).ifPresent(rol -> attachPermissionIfMissing(rol, permiso));
        }
    }

    private void attachPermissionIfMissing(Rol rol, Permiso permiso) {
        if (rolPermisoRepository.existsByRol_IdAndPermiso_Id(rol.getId(), permiso.getId())) {
            return;
        }

        RolPermiso rolPermiso = new RolPermiso();
        rolPermiso.setRol(rol);
        rolPermiso.setPermiso(permiso);
        rolPermisoRepository.save(rolPermiso);
    }
}
