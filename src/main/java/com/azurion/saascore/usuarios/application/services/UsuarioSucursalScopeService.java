package com.azurion.saascore.usuarios.application.services;

import com.azurion.saascore.sucursales.domain.repositories.SucursalRepository;
import com.azurion.saascore.usuarios.application.dto.UsuarioSucursalResponse;
import com.azurion.shared.exception.BusinessException;
import jakarta.persistence.EntityManager;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioSucursalScopeService {

    private final EntityManager entityManager;
    private final SucursalRepository sucursalRepository;

    public void sync(Long usuarioId, List<Long> rawSucursalIds) {
        if (rawSucursalIds == null) {
            return;
        }

        LinkedHashSet<Long> sucursalIds = rawSucursalIds.stream()
                .filter(id -> id != null && id > 0)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

        var sucursales = sucursalRepository.findAllById(sucursalIds);
        if (sucursales.size() != sucursalIds.size()) {
            throw new BusinessException("SUCURSAL_NO_ENCONTRADA", "Una o mas sucursales seleccionadas no existen");
        }
        if (sucursales.stream().anyMatch(sucursal -> !sucursal.isActivo())) {
            throw new BusinessException("SUCURSAL_INACTIVA", "No se puede asignar una sucursal inactiva al usuario");
        }

        entityManager.createNativeQuery("DELETE FROM usuario_sucursales WHERE usuario_id = ?")
                .setParameter(1, usuarioId)
                .executeUpdate();

        for (Long sucursalId : sucursalIds) {
            entityManager.createNativeQuery("""
                    INSERT INTO usuario_sucursales (usuario_id, sucursal_id)
                    VALUES (?, ?)
                    ON CONFLICT (usuario_id, sucursal_id) DO NOTHING
                    """)
                    .setParameter(1, usuarioId)
                    .setParameter(2, sucursalId)
                    .executeUpdate();
        }
    }

    @SuppressWarnings("unchecked")
    public List<UsuarioSucursalResponse> findByUsuarioId(Long usuarioId) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT s.id, s.codigo, s.nombre
                FROM usuario_sucursales us
                JOIN sucursales s ON s.id = us.sucursal_id
                WHERE us.usuario_id = ?
                ORDER BY s.nombre, s.id
                """)
                .setParameter(1, usuarioId)
                .getResultList();

        return rows.stream()
                .map(row -> new UsuarioSucursalResponse(
                        ((Number) row[0]).longValue(),
                        String.valueOf(row[1]),
                        String.valueOf(row[2])
                ))
                .toList();
    }
}
