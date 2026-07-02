package com.azurion.saascore.modulos.application.dto;

import java.util.List;

public record ActiveModulesResponse(
        Long empresaId,
        String tenantId,
        List<String> modules
) {
}
