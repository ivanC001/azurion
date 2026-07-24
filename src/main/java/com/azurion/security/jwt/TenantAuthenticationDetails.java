package com.azurion.security.jwt;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

public class TenantAuthenticationDetails extends WebAuthenticationDetails {

    private final Long userId;
    private final String tenantId;
    private final String sessionTenantId;
    private final String sessionId;

    public TenantAuthenticationDetails(
            HttpServletRequest request,
            Long userId,
            String tenantId,
            String sessionTenantId,
            String sessionId
    ) {
        super(request);
        this.userId = userId;
        this.tenantId = tenantId;
        this.sessionTenantId = sessionTenantId;
        this.sessionId = sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getSessionTenantId() {
        return sessionTenantId;
    }
}
