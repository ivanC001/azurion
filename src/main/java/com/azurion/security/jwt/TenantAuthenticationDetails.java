package com.azurion.security.jwt;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

public class TenantAuthenticationDetails extends WebAuthenticationDetails {

    private final Long userId;
    private final String tenantId;

    public TenantAuthenticationDetails(HttpServletRequest request, Long userId, String tenantId) {
        super(request);
        this.userId = userId;
        this.tenantId = tenantId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTenantId() {
        return tenantId;
    }
}
