package com.azurion.saascore.crm.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;

public record UpdateCrmLeadNotificationConfigRequest(
        Boolean activo,
        Boolean notificarLeadNuevo,
        Boolean notificarMensajeNuevo,
        Boolean notificarResponsable,
        List<String> correosCopia,
        @Min(0) @Max(10080) Integer cooldownMinutos
) {
}
