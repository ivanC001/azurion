package com.azurion.saascore.empresas.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateEmpresaRequest(
        @NotBlank @Size(min = 3, max = 40) @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._/-]{2,39}$") String ruc,
        @NotBlank @Size(max = 255) String razonSocial,
        @Size(max = 30) String tipoDocumentoFiscal,
        @Size(max = 180) String nombreComercial,
        @Pattern(regexp = "^[A-Za-z]{2}$") String paisCodigo,
        @Size(max = 100) String paisNombre,
        @Pattern(regexp = "^[A-Za-z]{3}$") String monedaCodigo,
        @Size(max = 10) String monedaSimbolo,
        @Size(max = 80) String zonaHoraria,
        @Size(max = 20) String idioma,
        @NotBlank @Pattern(regexp = "^[a-z][a-z0-9_]{2,62}$", message = "tenantId must be valid identifier") String tenantId,
        @NotBlank @Pattern(regexp = "^[a-z][a-z0-9_]{2,62}$", message = "schemaName must be valid postgres identifier") String schemaName,
        List<String> moduloCodigos
) {
}
