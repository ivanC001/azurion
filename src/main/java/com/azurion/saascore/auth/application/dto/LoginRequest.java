package com.azurion.saascore.auth.application.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password,
        String tenantId,
        @NotBlank String deviceId,
        String deviceName
) {
    public LoginRequest(String username, String password, String tenantId) {
        this(username, password, tenantId, null, null);
    }
}
