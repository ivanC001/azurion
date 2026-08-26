package com.azurion.saascore.crm.application.events;

public record WhatsappInboundMessageStoredEvent(
        String tenantId,
        Long dispatchId
) {
}
