package com.azurion.saascore.sucursales.application.services;

import com.azurion.saascore.sucursales.domain.entities.Sucursal;
import com.azurion.saascore.sucursales.domain.repositories.SucursalRepository;
import com.azurion.shared.exception.BusinessException;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SucursalOperationalGuard {

    private final EntityManager entityManager;
    private final SucursalRepository sucursalRepository;

    public void requireActive(Sucursal sucursal) {
        if (!sucursal.isActivo()) {
            throw new BusinessException(
                    "SUCURSAL_INACTIVA",
                    "La sucursal " + sucursal.getNombre() + " esta inactiva y no permite operaciones"
            );
        }
    }

    public void validateCanDeactivate(Sucursal sucursal) {
        if (!sucursal.isActivo()) {
            return;
        }
        if (sucursalRepository.countByActivoTrue() <= 1) {
            throw new BusinessException(
                    "ULTIMA_SUCURSAL_ACTIVA",
                    "No se puede deshabilitar la ultima sucursal activa de la empresa"
            );
        }

        long openCashRegisters = count("""
                SELECT COUNT(*)
                FROM cajas
                WHERE sucursal_id = ? AND UPPER(estado) = 'ABIERTA'
                """, sucursal.getId());
        if (openCashRegisters > 0) {
            throw new BusinessException(
                    "SUCURSAL_CON_CAJAS_ABIERTAS",
                    "Cierra las cajas abiertas de la sucursal antes de deshabilitarla"
            );
        }

        BigDecimal availableStock = total("""
                SELECT COALESCE(SUM(st.cantidad), 0)
                FROM stock st
                JOIN almacenes a ON a.id = st.almacen_id
                WHERE a.sucursal_id = ? AND st.cantidad > 0
                """, sucursal.getId());
        if (availableStock.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException(
                    "SUCURSAL_CON_STOCK",
                    "Transfiere o retira el stock disponible de la sucursal antes de deshabilitarla"
            );
        }

        long activeUsers = count("""
                SELECT COUNT(*)
                FROM usuario_sucursales us
                JOIN usuarios u ON u.id = us.usuario_id
                WHERE us.sucursal_id = ? AND u.activo = TRUE
                """, sucursal.getId());
        if (activeUsers > 0) {
            throw new BusinessException(
                    "SUCURSAL_CON_USUARIOS_ACTIVOS",
                    "Reasigna o deshabilita los usuarios activos de la sucursal antes de deshabilitarla"
            );
        }
    }

    private long count(String sql, Long sucursalId) {
        return ((Number) entityManager.createNativeQuery(sql)
                .setParameter(1, sucursalId)
                .getSingleResult()).longValue();
    }

    private BigDecimal total(String sql, Long sucursalId) {
        Object value = entityManager.createNativeQuery(sql)
                .setParameter(1, sucursalId)
                .getSingleResult();
        return value instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(value));
    }
}
