package com.azurion.saascore.auth.application.services;

import com.azurion.shared.exception.BusinessException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final EntityManager entityManager;

    public void validarSucursal(Long usuarioId, Long sucursalId) {
        validarAlcance(usuarioId, sucursalId, "usuario_sucursales", "sucursal_id", "SUCURSAL_NO_ASIGNADA");
    }

    public void validarAlmacen(Long usuarioId, Long almacenId) {
        validarAlcance(usuarioId, almacenId, "usuario_almacenes", "almacen_id", "ALMACEN_NO_ASIGNADO");
    }

    public void validarCaja(Long usuarioId, Long cajaId) {
        validarAlcance(usuarioId, cajaId, "usuario_cajas", "caja_id", "CAJA_NO_ASIGNADA");
    }

    public Long currentUsuarioId() {
        String value = MDC.get("userId");
        try {
            return value == null || value.isBlank() || "unknown".equals(value) ? null : Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public void asignarCaja(Long usuarioId, Long cajaId) {
        if (usuarioId != null && cajaId != null) {
            entityManager.createNativeQuery("""
                    INSERT INTO usuario_cajas (usuario_id, caja_id)
                    VALUES (?, ?)
                    ON CONFLICT (usuario_id, caja_id) DO NOTHING
                    """)
                    .setParameter(1, usuarioId)
                    .setParameter(2, cajaId)
                    .executeUpdate();
        }
    }

    private void validarAlcance(Long usuarioId, Long recursoId, String tabla, String columna, String codigoError) {
        if (recursoId == null) {
            throw new BusinessException(codigoError, "El usuario y el recurso son obligatorios");
        }
        if (esAdministrador()) {
            return;
        }
        if (usuarioId == null) {
            throw new BusinessException(codigoError, "No se pudo identificar al usuario autenticado");
        }

        Object resultado = entityManager.createNativeQuery(
                        "SELECT EXISTS (SELECT 1 FROM " + tabla + " WHERE usuario_id = ? AND " + columna + " = ?)")
                .setParameter(1, usuarioId)
                .setParameter(2, recursoId)
                .getSingleResult();
        boolean permitido = resultado instanceof Boolean value ? value : Boolean.parseBoolean(String.valueOf(resultado));
        if (!permitido) {
            throw new BusinessException(codigoError, "El recurso no esta asignado al usuario");
        }
    }

    private boolean esAdministrador() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> switch (authority.getAuthority()) {
                    case "ROLE_ADMIN_GENERAL", "ROLE_PLATFORM_ADMIN", "ROLE_ERP_ADMIN", "ROLE_CRM_ADMIN" -> true;
                    default -> false;
                });
    }
}
