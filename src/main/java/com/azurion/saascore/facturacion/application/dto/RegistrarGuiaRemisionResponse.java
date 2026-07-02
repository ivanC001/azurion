package com.azurion.saascore.facturacion.application.dto;

import com.azurion.saascore.caja.application.dto.FacturadorVentaResponse;

public record RegistrarGuiaRemisionResponse(
        String externalId,
        GuiaRemisionResponse guia,
        FacturadorVentaResponse facturacion
) {
}
