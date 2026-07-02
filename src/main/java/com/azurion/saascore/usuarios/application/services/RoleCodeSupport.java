package com.azurion.saascore.usuarios.application.services;

import java.util.Locale;
import org.springframework.security.core.Authentication;

public final class RoleCodeSupport {

    private RoleCodeSupport() {
    }

    public static String normalizeRoleCode(String rawRoleCode) {
        if (rawRoleCode == null) {
            return "";
        }

        String normalized = rawRoleCode.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring("ROLE_".length());
        }
        return normalized;
    }

    public static String toAuthority(String roleCode) {
        String normalized = normalizeRoleCode(roleCode);
        return normalized.isBlank() ? normalized : "ROLE_" + normalized;
    }

    public static boolean hasAnyRole(Authentication authentication, String... roleCodes) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        for (String roleCode : roleCodes) {
            String expected = toAuthority(roleCode);
            boolean present = authentication.getAuthorities().stream()
                    .map(authority -> authority.getAuthority())
                    .anyMatch(authority -> authority.equalsIgnoreCase(expected));
            if (present) {
                return true;
            }
        }

        return false;
    }
}
