package com.azurion.saascore.usuarios.application.usecases;

import com.azurion.saascore.auth.domain.repositories.UsuarioGlobalRepository;
import com.azurion.saascore.usuarios.application.dto.UsuarioTenantRolResponse;
import com.azurion.saascore.usuarios.application.services.TenantRoleAssignmentAuthorizer;
import com.azurion.saascore.usuarios.domain.repositories.UsuarioTenantRolRepository;
import com.azurion.shared.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListUsuarioTenantRolesUseCase {

    private final UsuarioGlobalRepository usuarioGlobalRepository;
    private final UsuarioTenantRolRepository usuarioTenantRolRepository;
    private final TenantRoleAssignmentAuthorizer tenantRoleAssignmentAuthorizer;

    public List<UsuarioTenantRolResponse> execute(Long usuarioId, String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new BusinessException("TENANT_INVALIDO", "tenantId es obligatorio");
        }

        tenantRoleAssignmentAuthorizer.assertCanRead(tenantId);

        if (!usuarioGlobalRepository.existsById(usuarioId)) {
            throw new BusinessException("USUARIO_NO_ENCONTRADO", "Usuario global no encontrado");
        }

        return usuarioTenantRolRepository.findByUsuarioGlobalIdAndTenantIdIgnoreCaseAndActivoTrue(usuarioId, tenantId)
                .stream()
                .map(entity -> new UsuarioTenantRolResponse(
                        entity.getId(),
                        entity.getUsuarioGlobal().getId(),
                        entity.getTenantId(),
                        entity.getRolCodigo(),
                        entity.isActivo(),
                        entity.getAsignadoPorUsuarioId()
                ))
                .toList();
    }
}
