package com.azurion.saascore.roles.application.support;

import com.azurion.saascore.roles.domain.entities.Permiso;
import com.azurion.saascore.roles.domain.entities.Rol;
import com.azurion.shared.exception.BusinessException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

public final class RolesBusinessRules {

    public static final Set<String> ADMIN_ROLE_CODES = Set.of("ADMIN", "ADMIN_EMPRESA");
    public static final Set<String> PROTECTED_ROLE_CODES = Set.of(
            "ADMIN",
            "ADMIN_EMPRESA",
            "SUPERVISOR_SUCURSAL",
            "CAJERO",
            "VENDEDOR",
            "ALMACENERO",
            "CONTADOR",
            "AUDITOR",
            "CRM_ADMIN",
            "CRM_GERENTE",
            "CRM_SUPERVISOR",
            "CRM_VENDEDOR",
            "CRM_MARKETING",
            "CRM_CALLCENTER"
    );

    private RolesBusinessRules() {
    }

    public static String normalizeRoleCode(String value) {
        String base = normalizeIdentifier(value);
        if (base.isBlank()) {
            return "";
        }
        return Character.isLetter(base.charAt(0)) ? base : "ROL_" + base;
    }

    public static String normalizeIdentifier(String value) {
        String normalized = value == null ? "" : Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "")
                .replaceAll("_+", "_");
        return normalized.length() > 80 ? normalized.substring(0, 80) : normalized;
    }

    public static boolean isAdministrativeRole(Rol rol) {
        return rol != null && ADMIN_ROLE_CODES.contains(normalizeIdentifier(rol.getCodigo()));
    }

    public static boolean isProtectedRole(Rol rol) {
        return rol != null && (rol.isSistema() || PROTECTED_ROLE_CODES.contains(normalizeIdentifier(rol.getCodigo())));
    }

    public static boolean canEditRole(Rol rol) {
        return !isAdministrativeRole(rol);
    }

    public static boolean canDeleteRole(Rol rol) {
        return !isProtectedRole(rol);
    }

    public static boolean canManagePermissions(Rol rol) {
        return !isAdministrativeRole(rol);
    }

    public static boolean isReservedRoleCode(String codigo) {
        return PROTECTED_ROLE_CODES.contains(normalizeRoleCode(codigo));
    }

    public static boolean canEditPermiso(Permiso permiso) {
        return permiso != null && !permiso.isSistema();
    }

    public static boolean canDeletePermiso(Permiso permiso) {
        return permiso != null && !permiso.isSistema();
    }

    public static void ensureCustomRoleCode(String codigo) {
        if (isReservedRoleCode(codigo)) {
            throw new BusinessException("ROL_RESERVADO", "Ese codigo esta reservado para roles base del sistema");
        }
    }
}
