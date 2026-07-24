package com.azurion.security.session;

import com.azurion.shared.audit.AuditPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthSessionAuditService {

    private final AuditPersistenceService auditPersistenceService;

    public void record(
            String event,
            String tenantId,
            Long userId,
            String sessionId,
            SessionClientInfo client,
            int status
    ) {
        try {
            String message = "event=" + clean(event, 50)
                    + "; sessionId=" + clean(sessionId, 80)
                    + "; deviceId=" + clean(client.deviceId(), 120)
                    + "; deviceName=" + clean(client.deviceName(), 120)
                    + "; ip=" + clean(client.ipAddress(), 80)
                    + "; userAgent=" + clean(client.userAgent(), 300);
            auditPersistenceService.save(
                    tenantId,
                    String.valueOf(userId),
                    "AUTH",
                    "/auth/session/" + clean(event.toLowerCase(), 50),
                    status,
                    0,
                    message
            );
        } catch (Exception ex) {
            log.error("No se pudo registrar auditoria de sesion event={} tenant={} userId={}",
                    event, tenantId, userId, ex);
        }
    }

    private String clean(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String sanitized = value.replaceAll("[\\r\\n\\t;]", " ").trim();
        return sanitized.substring(0, Math.min(maxLength, sanitized.length()));
    }
}
