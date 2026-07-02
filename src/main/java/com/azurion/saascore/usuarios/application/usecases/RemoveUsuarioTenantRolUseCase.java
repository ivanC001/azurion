package com.azurion.saascore.usuarios.application.usecases;

import com.azurion.saascore.usuarios.application.services.RoleCodeSupport;
import com.azurion.saascore.usuarios.application.services.TenantRoleAssignmentAuthorizer;
import com.azurion.saascore.usuarios.domain.entities.UsuarioTenantRol;
import com.azurion.saascore.usuarios.domain.repositories.UsuarioTenantRolRepository;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RemoveUsuarioTenantRolUseCase {

    private final UsuarioTenantRolRepository usuarioTenantRolRepository;
    private final TenantRoleAssignmentAuthorizer tenantRoleAssignmentAuthorizer;

    public void execute(Long usuarioId, String tenantId, String rawRolCodigo) {
        String normalizedRolCodigo = RoleCodeSupport.normalizeRoleCode(rawRolCodigo);
        if (normalizedRolCodigo.isBlank()) {
            throw new BusinessException("ROL_INVALIDO", "rolCodigo es obligatorio");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new BusinessException("TENANT_INVALIDO", "tenantId es obligatorio");
        }

        tenantRoleAssignmentAuthorizer.assertCanAssign(tenantId, normalizedRolCodigo);

        UsuarioTenantRol assignment = usuarioTenantRolRepository
                .findByUsuarioGlobalIdAndTenantIdIgnoreCaseAndRolCodigoIgnoreCaseAndActivoTrue(
                        usuarioId,
                        tenantId,
                        normalizedRolCodigo
                )
                .orElseThrow(() -> new BusinessException("ASIGNACION_NO_ENCONTRADA", "Asignacion de rol no encontrada"));

        usuarioTenantRolRepository.delete(assignment);
    }
}
