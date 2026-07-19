package com.azurion.saascore.crm.application.dto;

public record WhatsappWebhookResult(
        int mensajesProcesados,
        int mensajesDuplicados,
        int estadosActualizados
) {
}
