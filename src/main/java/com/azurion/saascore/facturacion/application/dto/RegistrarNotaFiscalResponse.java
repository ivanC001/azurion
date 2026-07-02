package com.azurion.saascore.facturacion.application.dto;

import com.azurion.saascore.caja.application.dto.FacturadorVentaResponse;

public record RegistrarNotaFiscalResponse(
        String externalId,
        NotaFiscalResponse nota,
        FacturadorVentaResponse facturacion
) {
}
