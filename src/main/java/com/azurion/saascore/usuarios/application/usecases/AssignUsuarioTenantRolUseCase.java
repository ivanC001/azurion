package com.azurion.saascore.usuarios.application.usecases;

import com.azurion.multitenancy.TenantSchemaRegistryRepository;
import com.azurion.saascore.auth.domain.entities.UsuarioGlobal;
import com.azurion.saascore.auth.domain.repositories.UsuarioGlobalRepository;
import com.azurion.saascore.usuarios.application.dto.UsuarioTenantRolResponse;
import com.azurion.saascore.usuarios.application.services.RoleCodeSupport;
import com.azurion.saascore.usuarios.application.services.TenantRoleAssignmentAuthorizer;
import com.azurion.saascore.usuarios.domain.entities.UsuarioTenantRol;
import com.azurion.saascore.usuarios.domain.repositories.UsuarioTenantRolRepository;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AssignUsuarioTenantRolUseCase {

    private final UsuarioGlobalRepository usuarioGlobalRepository;
    private final UsuarioTenantRolRepository usuarioTenantRolRepository;
    private final TenantSchemaRegistryRepository tenantSchemaRegistryRepository;
    private final TenantRoleAssignmentAuthorizer tenantRoleAssignmentAuthorizer;

    public UsuarioTenantRolResponse execute(Long usuarioId, String tenantId, String rawRolCodigo) {
        String normalizedTenantId = normalizeTenantId(tenantId);
        String normalizedRolCodigo = RoleCodeSupport.normalizeRoleCode(rawRolCodigo);

        if (normalizedRolCodigo.isBlank()) {
            throw new BusinessException("ROL_INVALIDO", "rolCodigo es obligatorio");
        }
        if ("ADMIN_GENERAL".equalsIgnoreCase(normalizedRolCodigo)
                || "PLATFORM_ADMIN".equalsIgnoreCase(normalizedRolCodigo)) {
            throw new BusinessException("ROL_INVALIDO", "Los roles globales no se asignan como rol de tenant");
        }

        tenantRoleAssignmentAuthorizer.assertCanAssign(normalizedTenantId, normalizedRolCodigo);

        tenantSchemaRegistryRepository.findByTenantIdAndActiveTrue(normalizedTenantId)
                .orElseThrow(() -> new BusinessException("TENANT_NO_ENCONTRADO", "Tenant no encontrado o inactivo"));

        UsuarioGlobal usuarioGlobal = usuarioGlobalRepository.findById(usuarioId)
                .orElseThrow(() -> new BusinessException("USUARIO_NO_ENCONTRADO", "Usuario global no encontrado"));

        UsuarioTenantRol existing = usuarioTenantRolRepository
                .findByUsuarioGlobalIdAndTenantIdIgnoreCaseAndRolCodigoIgnoreCaseAndActivoTrue(
                        usuarioId,
                        normalizedTenantId,
                        normalizedRolCodigo
                )
                .orElse(null);
        if (existing != null) {
            return toResponse(existing);
        }

        UsuarioTenantRol assignment = new UsuarioTenantRol();
        assignment.setUsuarioGlobal(usuarioGlobal);
        assignment.setTenantId(normalizedTenantId);
        assignment.setRolCodigo(normalizedRolCodigo);
        assignment.setActivo(true);
        assignment.setAsignadoPorUsuarioId(resolveActorUserId());

        return toResponse(usuarioTenantRolRepository.save(assignment));
    }

    private String normalizeTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new BusinessException("TENANT_INVALIDO", "tenantId es obligatorio");
        }
        return tenantId.trim();
    }

    private Long resolveActorUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return usuarioGlobalRepository.findByUsernameAndActivoTrue(authentication.getName())
                .map(UsuarioGlobal::getId)
                .orElse(null);
    }

    private UsuarioTenantRolResponse toResponse(UsuarioTenantRol entity) {
        return new UsuarioTenantRolResponse(
                entity.getId(),
                entity.getUsuarioGlobal().getId(),
                entity.getTenantId(),
                entity.getRolCodigo(),
                entity.isActivo(),
                entity.getAsignadoPorUsuarioId()
        );
    }
}
