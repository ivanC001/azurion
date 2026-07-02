package com.azurion.saascore.empresas.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public record CreateEmpresaRequest(
        @NotBlank @Pattern(regexp = "^[0-9]{11}$") String ruc,
        @NotBlank String razonSocial,
        @NotBlank @Pattern(regexp = "^[a-z][a-z0-9_]{2,62}$", message = "tenantId must be valid identifier") String tenantId,
        @NotBlank @Pattern(regexp = "^[a-z][a-z0-9_]{2,62}$", message = "schemaName must be valid postgres identifier") String schemaName,
        List<String> moduloCodigos
) {
}
