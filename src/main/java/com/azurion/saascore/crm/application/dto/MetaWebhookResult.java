package com.azurion.saascore.crm.application.dto;

public record MetaWebhookResult(
        String canal,
        int eventosRecibidos
) {
}
