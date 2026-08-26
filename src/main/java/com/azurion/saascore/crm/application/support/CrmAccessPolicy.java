package com.azurion.saascore.crm.application.support;

import com.azurion.saascore.auth.application.services.AuthorizationService;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Reglas de visibilidad del CRM: quien puede leer o escribir un registro segun
 * su responsable.
 *
 * Estaban dispersas como helpers privados dentro de CrmUseCaseService, lo que
 * obligaba a duplicarlas en cualquier caso de uso que se separase de el. Al ser
 * una decision de autorizacion conviene que tenga un unico dueno.
 */
@Component
@RequiredArgsConstructor
public class CrmAccessPolicy {

    /** Responsable ficticio de los leads que entran por formularios publicos. */
    public static final String PUBLIC_LEAD_OWNER = "crm-public";

    private static final String FALLBACK_USER = "system";

    private final AuthorizationService authorizationService;

    public void ensureCanRead(String owner) {
        if (PUBLIC_LEAD_OWNER.equals(owner) && canReadPublicLeadQueue()) {
            return;
        }
        if (!canViewAll() && !currentUserKey().equals(owner)) {
            throw new BusinessException("CRM_SIN_ACCESO", "No tienes acceso a este registro CRM");
        }
    }

    public void ensureCanWrite(String owner) {
        if (PUBLIC_LEAD_OWNER.equals(owner) && canWritePublicLeadQueue()) {
            return;
        }
        ensureCanRead(owner);
    }

    public boolean canViewAll() {
        return hasAuthority("CRM_VIEW_ALL")
                || hasAuthority("ROLE_ADMIN_GENERAL")
                || hasAuthority("ROLE_PLATFORM_ADMIN");
    }

    public boolean canReadPublicLeadQueue() {
        return canViewAll()
                || hasAuthority("CRM_LEADS_READ")
                || hasAuthority("CRM_ACTIVITIES_READ");
    }

    public boolean canWritePublicLeadQueue() {
        return canViewAll()
                || hasAuthority("CRM_LEADS_WRITE")
                || hasAuthority("CRM_ACTIVITIES_WRITE");
    }

    /**
     * Identidad con la que se marca el responsable de un registro CRM.
     */
    public String currentUserKey() {
        Long usuarioId = authorizationService.currentUsuarioId();
        if (usuarioId != null) {
            return String.valueOf(usuarioId);
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            return authentication.getName();
        }
        return FALLBACK_USER;
    }

    /**
     * Ambito de consulta: null cuando el usuario ve todo el CRM, o su propia
     * clave cuando solo puede ver lo suyo.
     */
    public String ownerScope() {
        return canViewAll() ? null : currentUserKey();
    }

    public boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }
}
