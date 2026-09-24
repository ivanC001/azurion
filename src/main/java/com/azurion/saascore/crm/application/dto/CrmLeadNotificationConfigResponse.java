package com.azurion.saascore.crm.application.dto;

import java.util.List;

public record CrmLeadNotificationConfigResponse(
        boolean activo,
        boolean notificarLeadNuevo,
        boolean notificarMensajeNuevo,
        boolean notificarResponsable,
        List<String> correosCopia,
        int cooldownMinutos,
        boolean correoTenantListo,
        String correoTenantDetalle
) {
}
