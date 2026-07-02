package com.azurion.saascore.usuarios.application.usecases;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.roles.domain.entities.Rol;
import com.azurion.saascore.roles.domain.repositories.RolRepository;
import com.azurion.saascore.usuarios.application.dto.UsuarioTenantResponse;
import com.azurion.saascore.usuarios.application.mappers.UsuariosMapper;
import com.azurion.saascore.usuarios.application.services.RoleCodeSupport;
import com.azurion.saascore.usuarios.application.services.TenantRoleAssignmentAuthorizer;
import com.azurion.saascore.usuarios.application.services.UsuarioSucursalScopeService;
import com.azurion.saascore.usuarios.domain.entities.UsuarioRol;
import com.azurion.saascore.usuarios.domain.entities.UsuarioTenant;
import com.azurion.saascore.usuarios.domain.repositories.UsuarioTenantRepository;
import com.azurion.shared.exception.BusinessException;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SyncUsuarioTenantRolesUseCase {

    private final UsuarioTenantRepository usuarioTenantRepository;
    private final RolRepository rolRepository;
    private final TenantRoleAssignmentAuthorizer tenantRoleAssignmentAuthorizer;
    private final UsuarioSucursalScopeService usuarioSucursalScopeService;

    @Transactional
    public UsuarioTenantResponse execute(Long usuarioId, List<String> rawRoleCodes) {
        UsuarioTenant usuario = usuarioTenantRepository.findWithUsuarioRolesById(usuarioId)
                .orElseThrow(() -> new BusinessException("USUARIO_NO_ENCONTRADO", "Usuario no encontrado"));

        LinkedHashSet<String> roleCodes = normalizeRoleCodes(rawRoleCodes);
        if (roleCodes.isEmpty()) {
            throw new BusinessException("ROLES_REQUERIDOS", "Debe enviar al menos un rol");
        }

        usuario.getUsuarioRoles().clear();
        // Flush orphan removals before inserting the replacement set so the
        // unique constraint (usuario_id, rol_id) cannot collide with old rows.
        usuarioTenantRepository.saveAndFlush(usuario);

        for (String roleCode : roleCodes) {
            tenantRoleAssignmentAuthorizer.assertCanAssign(TenantContext.getTenantId(), roleCode);
            Rol rol = rolRepository.findByCodigoIgnoreCase(roleCode)
                    .orElseThrow(() -> new BusinessException("ROL_NO_ENCONTRADO", "Rol no encontrado: " + roleCode));

            UsuarioRol usuarioRol = new UsuarioRol();
            usuarioRol.setUsuario(usuario);
            usuarioRol.setRol(rol);
            usuario.getUsuarioRoles().add(usuarioRol);
        }

        UsuarioTenant saved = usuarioTenantRepository.saveAndFlush(usuario);
        return UsuariosMapper.toResponse(saved, usuarioSucursalScopeService.findByUsuarioId(saved.getId()));
    }

    private LinkedHashSet<String> normalizeRoleCodes(List<String> rawRoleCodes) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (rawRoleCodes == null) {
            return normalized;
        }

        for (String rawRoleCode : rawRoleCodes) {
            String code = RoleCodeSupport.normalizeRoleCode(rawRoleCode);
            if (!code.isBlank()) {
                normalized.add(code);
            }
        }
        return normalized;
    }
}
