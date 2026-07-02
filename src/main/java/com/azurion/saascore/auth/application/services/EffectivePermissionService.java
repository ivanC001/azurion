package com.azurion.saascore.auth.application.services;

import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EffectivePermissionService {

    private final EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<String> findPermissionCodes(Long usuarioId) {
        return ((List<Object>) entityManager.createNativeQuery("""
                SELECT DISTINCT codigo
                FROM (
                    SELECT p.codigo
                    FROM usuario_roles ur
                    JOIN roles r ON r.id = ur.rol_id AND r.activo = TRUE
                    JOIN rol_permisos rp ON rp.rol_id = r.id
                    JOIN permisos p ON p.id = rp.permiso_id AND p.activo = TRUE
                    WHERE ur.usuario_id = ?

                    UNION

                    SELECT p.codigo
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
                ORDER BY codigo
                """)
                .setParameter(1, usuarioId)
                .setParameter(2, usuarioId)
                .setParameter(3, usuarioId)
                .getResultList()).stream()
                .map(String::valueOf)
                .toList();
    }
}
