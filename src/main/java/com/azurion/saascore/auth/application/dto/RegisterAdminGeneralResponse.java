package com.azurion.saascore.auth.application.dto;

import java.util.List;

public record RegisterAdminGeneralResponse(
        Long id,
        String username,
        String tenantId,
        List<String> roles
) {
}
