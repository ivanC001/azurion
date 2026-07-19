package com.azurion.saascore.crm.application.dto;

public record CrmInboxChannelResponse(
        String canal,
        String nombre,
        boolean activo
) {
}
