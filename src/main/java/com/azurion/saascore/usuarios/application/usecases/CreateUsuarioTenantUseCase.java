package com.azurion.saascore.usuarios.application.usecases;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.roles.domain.entities.Rol;
import com.azurion.saascore.roles.domain.repositories.RolRepository;
import com.azurion.saascore.usuarios.application.dto.CreateUsuarioTenantRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateUsuarioTenantUseCase {

    private final UsuarioTenantRepository usuarioTenantRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantRoleAssignmentAuthorizer tenantRoleAssignmentAuthorizer;
    private final UsuarioSucursalScopeService usuarioSucursalScopeService;

    @Transactional
    public UsuarioTenantResponse execute(CreateUsuarioTenantRequest request) {
        String username = request.username().trim();
        if (usuarioTenantRepository.existsByUsernameIgnoreCase(username)) {
            throw new BusinessException("USUARIO_DUPLICADO", "Ya existe un usuario con ese username");
        }

        UsuarioTenant usuario = new UsuarioTenant();
        usuario.setUsername(username);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setNombres(request.nombres().trim());
        usuario.setEmail(request.email() == null ? null : request.email().trim());
        usuario.setActivo(true);

        assignRoles(usuario, request.rolCodigos());

        UsuarioTenant saved = usuarioTenantRepository.save(usuario);
        usuarioSucursalScopeService.sync(saved.getId(), request.sucursalIds());
        return UsuariosMapper.toResponse(saved, usuarioSucursalScopeService.findByUsuarioId(saved.getId()));
    }

    private void assignRoles(UsuarioTenant usuario, List<String> roleCodes) {
        LinkedHashSet<String> normalized = normalizeRoleCodes(roleCodes);
        if (normalized.isEmpty()) {
            normalized.add("VENDEDOR");
        }

        for (String code : normalized) {
            tenantRoleAssignmentAuthorizer.assertCanAssign(TenantContext.getTenantId(), code);
            Rol rol = rolRepository.findByCodigoIgnoreCase(code)
                    .orElseThrow(() -> new BusinessException("ROL_NO_ENCONTRADO", "Rol no encontrado: " + code));
            UsuarioRol usuarioRol = new UsuarioRol();
            usuarioRol.setUsuario(usuario);
            usuarioRol.setRol(rol);
            usuario.getUsuarioRoles().add(usuarioRol);
        }
    }

    private LinkedHashSet<String> normalizeRoleCodes(List<String> roleCodes) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (roleCodes == null) {
            return normalized;
        }

        for (String roleCode : roleCodes) {
            String code = RoleCodeSupport.normalizeRoleCode(roleCode);
            if (!code.isBlank()) {
                normalized.add(code);
            }
        }
        return normalized;
    }
}
