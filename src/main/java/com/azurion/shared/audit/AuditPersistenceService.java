package com.azurion.shared.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditPersistenceService {

    private final AuditoriaGlobalRepository repository;

    public void save(String tenantId,
                     String userId,
                     String method,
                     String path,
                     int statusCode,
                     long durationMs,
                     String message) {
        AuditoriaGlobal record = new AuditoriaGlobal();
        record.setTenantId(tenantId);
        record.setUserId(userId);
        record.setMethod(method);
        record.setPath(path);
        record.setStatusCode(statusCode);
        record.setDurationMs(durationMs);
        record.setMessage(message);
        repository.save(record);
    }
}
