package com.azurion.saascore.modulos.application.services;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.auth.application.services.AuthorizationService;
import com.azurion.shared.audit.AuditPersistenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlatformModuleAuditService {

    private final AuditPersistenceService auditPersistenceService;
    private final AuthorizationService authorizationService;

    public void record(String path, String message) {
        Long currentUserId = authorizationService.currentUsuarioId();
        String tenantId = TenantContext.getTenantId() == null ? TenantContext.DEFAULT_TENANT : TenantContext.getTenantId();

        auditPersistenceService.save(
                tenantId,
                currentUserId == null ? "unknown" : String.valueOf(currentUserId),
                "DOMAIN",
                path,
                200,
                0,
                message
        );
    }
}
