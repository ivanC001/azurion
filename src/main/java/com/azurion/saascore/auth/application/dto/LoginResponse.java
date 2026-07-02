package com.azurion.saascore.auth.application.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String username,
        String tenantId,
        List<String> roles,
        List<String> permissions,
        List<String> modules,
        boolean adminGeneral,
        boolean adminEmpresa,
        OffsetDateTime issuedAt
) {
}
