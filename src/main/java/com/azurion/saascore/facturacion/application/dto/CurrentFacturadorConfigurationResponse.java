package com.azurion.saascore.facturacion.application.dto;

import com.azurion.saascore.empresas.application.dto.EmpresaResponse;
import com.fasterxml.jackson.databind.JsonNode;

public record CurrentFacturadorConfigurationResponse(
        JsonNode tenant,
        EmpresaResponse empresa
) {
}
