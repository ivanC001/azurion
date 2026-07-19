package com.azurion.saascore.auth.application.services;

import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EffectivePermissionService {

    private final EntityManager entityManager;
    private final PermissionModulePolicy permissionModulePolicy;

    @SuppressWarnings("unchecked")
    public List<String> findPermissionCodes(Long usuarioId) {
        return findPermissionGrants(usuarioId).stream()
                .map(PermissionGrant::code)
                .toList();
    }

    public List<String> findPermissionCodes(Long usuarioId, Collection<String> activeModules) {
        return findPermissionGrants(usuarioId).stream()
                .filter(grant -> permissionModulePolicy.isAllowed(grant.module(), activeModules))
                .map(PermissionGrant::code)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<PermissionGrant> findPermissionGrants(Long usuarioId) {
        return ((List<Object[]>) entityManager.createNativeQuery("""
                SELECT DISTINCT codigo, modulo
                FROM (
                    SELECT p.codigo, p.modulo
                    FROM usuario_roles ur
                    JOIN roles r ON r.id = ur.rol_id AND r.activo = TRUE
                    JOIN rol_permisos rp ON rp.rol_id = r.id
                    JOIN permisos p ON p.id = rp.permiso_id AND p.activo = TRUE
                    WHERE ur.usuario_id = ?

                    UNION

                    SELECT p.codigo, p.modulo
                    FROM usuario_permisos_especiales upe
                    JOIN permisos p ON p.id = upe.permiso_id AND p.activo = TRUE
                    WHERE upe.usuario_id = ? AND upe.tipo = 'GRANT'
                ) permisos_efectivos
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM usuario_permisos_especiales denegado
                    JOIN permisos p_denegado ON p_denegado.id = denegado.permiso_id
                    WHERE denegado.usuario_id = ?
                      AND denegado.tipo = 'DENY'
                      AND p_denegado.codigo = permisos_efectivos.codigo
                )
                ORDER BY codigo, modulo
                """)
                .setParameter(1, usuarioId)
                .setParameter(2, usuarioId)
                .setParameter(3, usuarioId)
                .getResultList()).stream()
                .map(row -> new PermissionGrant(String.valueOf(row[0]), String.valueOf(row[1])))
                .toList();
    }

    private record PermissionGrant(String code, String module) {
    }
}
