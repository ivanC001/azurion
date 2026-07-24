package com.azurion.saascore.crm.application.dto;

import com.azurion.saascore.cotizaciones.application.dto.CotizacionResponse;

public record SendWhatsappQuoteResponse(
        CrmWhatsappMessageResponse mensaje,
        CotizacionResponse cotizacion
) {
}
