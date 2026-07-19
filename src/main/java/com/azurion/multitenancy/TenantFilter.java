package com.azurion.multitenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.azurion.shared.exception.BusinessException;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private static final String TENANT_PATTERN = "^[a-z][a-z0-9_]{2,62}$";

    private final TenantSchemaLookupService tenantSchemaLookupService;

    @Value("${azurion.multitenancy.tenant-header:X-Tenant-Id}")
    private String tenantHeader;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String tenantId = normalizeAndValidateTenant(request.getHeader(tenantHeader));
            tenantSchemaLookupService.resolveSchema(tenantId);
            TenantContext.setTenantId(tenantId);
            MDC.put("tenant", tenantId);
            filterChain.doFilter(request, response);
        } catch (BusinessException ex) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":\"TENANT_ACCESS_DENIED\",\"message\":\"El tenant solicitado no existe, esta inactivo o no esta disponible\",\"details\":[],\"userActionable\":true}");
        } finally {
            TenantContext.clear();
            MDC.remove("tenant");
        }
    }

    private String normalizeAndValidateTenant(String rawTenantId) {
        if (rawTenantId == null || rawTenantId.isBlank()) {
            return TenantContext.DEFAULT_TENANT;
        }
        String tenantId = rawTenantId.trim().toLowerCase(java.util.Locale.ROOT);
        if (!TenantContext.DEFAULT_TENANT.equals(tenantId) && !tenantId.matches(TENANT_PATTERN)) {
            throw new BusinessException(
                    "TENANT_INVALIDO",
                    "El identificador de tenant no tiene un formato valido"
            );
        }
        return tenantId;
    }
}
