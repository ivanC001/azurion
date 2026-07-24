package com.azurion.saascore.usuarios.application.dto;

public record TenantUserQuotaResponse(
        long activeUsers,
        int limit,
        long remaining,
        String planCode
) {
}
